package br.gov.alvara.service;

import br.gov.alvara.dto.request.AtualizarStatusProcessoRequest;
import br.gov.alvara.dto.request.CriarProcessoRequest;
import br.gov.alvara.dto.response.AnaliseIAResponse;
import br.gov.alvara.dto.response.DocumentoResponse;
import br.gov.alvara.dto.response.ProcessoResponse;
import br.gov.alvara.entity.Processo;
import br.gov.alvara.entity.TipoEstabelecimento;
import br.gov.alvara.entity.Usuario;
import br.gov.alvara.enums.StatusProcesso;
import br.gov.alvara.exception.RecursoNaoEncontradoException;
import br.gov.alvara.exception.RegraDeNegocioException;
import br.gov.alvara.integration.s3.S3Service;
import br.gov.alvara.repository.AnaliseIARepository;
import br.gov.alvara.repository.ProcessoRepository;
import br.gov.alvara.repository.TipoEstabelecimentoRepository;
import br.gov.alvara.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessoService {

    private final ProcessoRepository processoRepository;
    private final TipoEstabelecimentoRepository tipoEstabelecimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnaliseIARepository analiseIARepository;
    private final S3Service s3Service;

    // -------------------------------------------------------------------------
    // CRIAR
    // -------------------------------------------------------------------------

    @Transactional
    public ProcessoResponse criar(CriarProcessoRequest request) {
        TipoEstabelecimento tipo = tipoEstabelecimentoRepository
                .findById(request.getTipoEstabelecimentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "TipoEstabelecimento", request.getTipoEstabelecimentoId()));

        Usuario usuarioLogado = getUsuarioLogado();

        Processo processo = Processo.builder()
                .nomeEmpresa(request.getNomeEmpresa())
                .cnpj(request.getCnpj())
                .tipoEstabelecimento(tipo)
                .criadoPor(usuarioLogado)
                .status(StatusProcesso.RASCUNHO)
                .observacoes(request.getObservacoes())
                .build();

        Processo salvo = processoRepository.save(processo);
        log.info("Processo criado: id={}, empresa={}, cnpj={}", salvo.getId(), salvo.getNomeEmpresa(), salvo.getCnpj());
        return toResponse(salvo);
    }

    // -------------------------------------------------------------------------
    // LISTAR / BUSCAR
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ProcessoResponse> listar(StatusProcesso status, String cnpj, String nomeEmpresa, Pageable pageable) {
        return processoRepository
                .buscarComFiltros(status, cnpj, nomeEmpresa, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProcessoResponse buscarPorId(Long id) {
        Processo processo = processoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", id));
        return toResponseCompleto(processo);
    }

    // -------------------------------------------------------------------------
    // ATUALIZAR STATUS (por servidor/admin)
    // -------------------------------------------------------------------------

    @Transactional
    public ProcessoResponse atualizarStatus(Long id, AtualizarStatusProcessoRequest request) {
        Processo processo = processoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", id));

        validarTransicaoStatus(processo.getStatus(), request.getStatus());

        processo.setStatus(request.getStatus());
        if (request.getObservacoes() != null) {
            processo.setObservacoes(request.getObservacoes());
        }

        Processo salvo = processoRepository.save(processo);
        log.info("Status do processo id={} alterado para: {}", id, request.getStatus());
        return toResponse(salvo);
    }

    // -------------------------------------------------------------------------
    // VALIDAÇÃO DE TRANSIÇÃO DE STATUS
    // -------------------------------------------------------------------------

    private void validarTransicaoStatus(StatusProcesso atual, StatusProcesso novo) {
        // Processos encerrados não podem ser reabertos por esta rota
        if (atual == StatusProcesso.APROVADO || atual == StatusProcesso.INDEFERIDO) {
            throw new RegraDeNegocioException(
                    "Processo encerrado (status: " + atual + ") não pode ter status alterado.");
        }
        // Não permitir aprovação automática via API sem análise
        if (novo == StatusProcesso.APROVADO && atual != StatusProcesso.AGUARDANDO_ANALISE) {
            throw new RegraDeNegocioException(
                    "Só é possível aprovar processos no status AGUARDANDO_ANALISE.");
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Usuario getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário logado não encontrado: " + email));
    }

    // Response simplificado (sem documentos nem análise — para listagens)
    private ProcessoResponse toResponse(Processo p) {
        return ProcessoResponse.builder()
                .id(p.getId())
                .nomeEmpresa(p.getNomeEmpresa())
                .cnpj(p.getCnpj())
                .tipoEstabelecimento(p.getTipoEstabelecimento().getNome())
                .status(p.getStatus())
                .dataCriacao(p.getDataCriacao())
                .dataAtualizacao(p.getDataAtualizacao())
                .criadoPor(p.getCriadoPor().getNome())
                .observacoes(p.getObservacoes())
                .build();
    }

    // Response completo (para busca por ID — inclui documentos e última análise)
    private ProcessoResponse toResponseCompleto(Processo p) {
        var documentos = p.getDocumentos().stream()
                .map(doc -> DocumentoResponse.builder()
                        .id(doc.getId())
                        .nome(doc.getNome())
                        .tipo(doc.getTipo())
                        .contentType(doc.getContentType())
                        .tamanhoBytes(doc.getTamanhoBytes())
                        .urlDownload(s3Service.gerarUrlPresignada(doc.getS3Key()))
                        .enviadoEm(doc.getEnviadoEm())
                        .enviadoPor(doc.getEnviadoPor().getNome())
                        .build())
                .toList();

        AnaliseIAResponse ultimaAnalise = analiseIARepository
                .findTopByProcessoIdOrderByDataAnaliseDesc(p.getId())
                .map(a -> AnaliseIAResponse.builder()
                        .id(a.getId())
                        .processoId(p.getId())
                        .statusGeral(a.getStatusGeral())
                        .resultadoJson(a.getResultadoJson())
                        .modeloUtilizado(a.getModeloUtilizado())
                        .dataAnalise(a.getDataAnalise())
                        .mensagemErro(a.getMensagemErro())
                        .build())
                .orElse(null);

        return ProcessoResponse.builder()
                .id(p.getId())
                .nomeEmpresa(p.getNomeEmpresa())
                .cnpj(p.getCnpj())
                .tipoEstabelecimento(p.getTipoEstabelecimento().getNome())
                .status(p.getStatus())
                .dataCriacao(p.getDataCriacao())
                .dataAtualizacao(p.getDataAtualizacao())
                .criadoPor(p.getCriadoPor().getNome())
                .observacoes(p.getObservacoes())
                .documentos(documentos)
                .ultimaAnalise(ultimaAnalise)
                .build();
    }
}
