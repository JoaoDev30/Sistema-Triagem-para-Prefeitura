package br.gov.alvara.service;

import br.gov.alvara.dto.response.AnaliseIAResponse;
import br.gov.alvara.entity.AnaliseIA;
import br.gov.alvara.entity.Documento;
import br.gov.alvara.entity.Processo;
import br.gov.alvara.entity.RegraDocumento;
import br.gov.alvara.enums.StatusAnalise;
import br.gov.alvara.enums.StatusProcesso;
import br.gov.alvara.exception.RecursoNaoEncontradoException;
import br.gov.alvara.exception.RegraDeNegocioException;
import br.gov.alvara.integration.openai.OpenAIClient;
import br.gov.alvara.repository.AnaliseIARepository;
import br.gov.alvara.repository.ProcessoRepository;
import br.gov.alvara.repository.RegraDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnaliseIAService {

    private final ProcessoRepository processoRepository;
    private final AnaliseIARepository analiseIARepository;
    private final RegraDocumentoRepository regraDocumentoRepository;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.model}")
    private String modeloIA;

    // -------------------------------------------------------------------------
    // PONTO DE ENTRADA: dispara análise completa de um processo
    // -------------------------------------------------------------------------

    @Transactional
    public AnaliseIAResponse analisarProcesso(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        // Regra: só analisa processos em estado adequado
        if (processo.getStatus() == StatusProcesso.APROVADO ||
            processo.getStatus() == StatusProcesso.INDEFERIDO) {
            throw new RegraDeNegocioException(
                    "Processo já encerrado (status: " + processo.getStatus() + ") não pode ser reanalisado.");
        }

        processo.setStatus(StatusProcesso.EM_ANALISE);
        processoRepository.save(processo);

        AnaliseIA analise = AnaliseIA.builder()
                .processo(processo)
                .modeloUtilizado(modeloIA)
                .statusGeral(StatusAnalise.PENDENTE)
                .build();

        try {
            // 1. Validar documentos obrigatórios
            List<String> documentosFaltando = validarDocumentosObrigatorios(processo);

            // 2. Montar contexto textual dos documentos enviados
            String contextoDocumentos = montarContextoDocumentos(processo.getDocumentos());

            // 3. Construir prompt dinâmico com base no tipo de estabelecimento
            String tipoEstabelecimento = processo.getTipoEstabelecimento().getNome();
            String promptSistema = montarPromptSistema(tipoEstabelecimento);
            String promptUsuario = montarPromptUsuario(processo, contextoDocumentos, documentosFaltando);

            // 4. Chamar IA
            log.info("Iniciando análise IA para processo id={}, tipo={}", processoId, tipoEstabelecimento);
            String respostaIA = openAIClient.enviarPrompt(promptSistema, promptUsuario);

            // 5. Persistir resultado
            analise.setPromptEnviado(promptUsuario);
            analise.setResultadoJson(respostaIA);
            analise.setStatusGeral(StatusAnalise.CONCLUIDA);

            // 6. Atualizar status do processo
            // REGRA DE NEGÓCIO: nunca aprovar automaticamente
            StatusProcesso novoStatus = documentosFaltando.isEmpty()
                    ? StatusProcesso.AGUARDANDO_ANALISE  // Servidor precisa revisar
                    : StatusProcesso.PENDENTE;           // Documentos faltando
            processo.setStatus(novoStatus);

        } catch (Exception e) {
            log.error("Erro durante análise IA do processo {}: {}", processoId, e.getMessage(), e);
            analise.setStatusGeral(StatusAnalise.ERRO);
            analise.setMensagemErro("Falha na análise: " + e.getMessage());
            processo.setStatus(StatusProcesso.PENDENTE);
        }

        processoRepository.save(processo);
        AnaliseIA salva = analiseIARepository.save(analise);
        log.info("Análise IA concluída para processo id={}. Status: {}", processoId, analise.getStatusGeral());

        return toResponse(salva);
    }

    // -------------------------------------------------------------------------
    // VALIDAÇÃO DE DOCUMENTOS
    // -------------------------------------------------------------------------

    private List<String> validarDocumentosObrigatorios(Processo processo) {
        List<RegraDocumento> regras = regraDocumentoRepository
                .findByTipoEstabelecimentoIdAndObrigatorio(
                        processo.getTipoEstabelecimento().getId(), true);

        List<String> tiposEnviados = processo.getDocumentos()
                .stream()
                .map(Documento::getTipo)
                .toList();

        List<String> faltando = regras.stream()
                .map(RegraDocumento::getNomeDocumento)
                .filter(nomeRequerido -> tiposEnviados.stream()
                        .noneMatch(tipoEnviado -> tipoEnviado.equalsIgnoreCase(nomeRequerido)))
                .collect(Collectors.toList());

        if (!faltando.isEmpty()) {
            log.warn("Processo id={} com documentos faltando: {}", processo.getId(), faltando);
        }
        return faltando;
    }

    // -------------------------------------------------------------------------
    // CONSTRUÇÃO DE PROMPTS DINÂMICOS
    // -------------------------------------------------------------------------

    private String montarPromptSistema(String tipoEstabelecimento) {
        return """
                Você é um especialista em vigilância sanitária brasileira.
                Sua tarefa é analisar documentos de um processo de solicitação de Alvará Sanitário
                para um estabelecimento do tipo: %s.
                
                Retorne APENAS um JSON válido, sem nenhum texto adicional, com a seguinte estrutura:
                {
                  "parecer_geral": "string - análise geral do processo",
                  "conformidades": ["lista de pontos conformes"],
                  "nao_conformidades": ["lista de pontos não conformes"],
                  "documentos_faltando": ["lista de documentos ausentes"],
                  "exigencias": ["lista de exigências a serem cumpridas"],
                  "risco_sanitario": "BAIXO | MÉDIO | ALTO",
                  "recomendacao": "APROVACAO_PENDENTE_REVISAO | EXIGENCIA | INDEFERIMENTO",
                  "observacoes_tecnicas": "string - observações técnicas detalhadas"
                }
                
                IMPORTANTE: Nunca recomendar aprovação automática. Sempre retornar APROVACAO_PENDENTE_REVISAO
                para indicar que um servidor humano deve revisar antes da decisão final.
                """.formatted(tipoEstabelecimento);
    }

    private String montarPromptUsuario(
            Processo processo,
            String contextoDocumentos,
            List<String> documentosFaltando) {

        String faltandoStr = documentosFaltando.isEmpty()
                ? "Nenhum documento obrigatório identificado como faltante."
                : "ATENÇÃO - Documentos obrigatórios NÃO enviados: " + String.join(", ", documentosFaltando);

        return """
                DADOS DO PROCESSO:
                - Empresa: %s
                - CNPJ: %s
                - Tipo de Estabelecimento: %s
                - Status atual: %s
                
                SITUAÇÃO DOS DOCUMENTOS:
                %s
                
                DOCUMENTOS ENVIADOS E SEUS CONTEÚDOS:
                %s
                
                Analise os dados acima e gere o parecer técnico conforme o formato solicitado.
                """.formatted(
                processo.getNomeEmpresa(),
                processo.getCnpj(),
                processo.getTipoEstabelecimento().getNome(),
                processo.getStatus().name(),
                faltandoStr,
                contextoDocumentos
        );
    }

    private String montarContextoDocumentos(List<Documento> documentos) {
        if (documentos.isEmpty()) {
            return "Nenhum documento foi enviado para este processo.";
        }
        StringBuilder sb = new StringBuilder();
        for (Documento doc : documentos) {
            sb.append("- [").append(doc.getTipo()).append("] ")
              .append(doc.getNome())
              .append(" (enviado em: ").append(doc.getEnviadoEm()).append(")\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // MAPEAMENTO PARA RESPONSE
    // -------------------------------------------------------------------------

    public AnaliseIAResponse buscarUltimaAnalise(Long processoId) {
        return analiseIARepository
                .findTopByProcessoIdOrderByDataAnaliseDesc(processoId)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma análise encontrada para o processo id: " + processoId));
    }

    private AnaliseIAResponse toResponse(AnaliseIA analise) {
        Object resultadoParsed = analise.getResultadoJson();
        try {
            if (analise.getResultadoJson() != null) {
                resultadoParsed = objectMapper.readValue(analise.getResultadoJson(), Object.class);
            }
        } catch (Exception e) {
            log.warn("Resultado da IA não é JSON válido, retornando como texto");
        }

        return AnaliseIAResponse.builder()
                .id(analise.getId())
                .processoId(analise.getProcesso().getId())
                .statusGeral(analise.getStatusGeral())
                .resultadoJson(resultadoParsed)
                .modeloUtilizado(analise.getModeloUtilizado())
                .dataAnalise(analise.getDataAnalise())
                .mensagemErro(analise.getMensagemErro())
                .build();
    }
}
