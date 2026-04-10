package br.gov.alvara.service;

import br.gov.alvara.dto.response.DocumentoResponse;
import br.gov.alvara.entity.Documento;
import br.gov.alvara.entity.Processo;
import br.gov.alvara.entity.Usuario;
import br.gov.alvara.exception.RecursoNaoEncontradoException;
import br.gov.alvara.exception.RegraDeNegocioException;
import br.gov.alvara.integration.s3.S3Service;
import br.gov.alvara.repository.DocumentoRepository;
import br.gov.alvara.repository.ProcessoRepository;
import br.gov.alvara.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/tiff"
    );

    private final DocumentoRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final S3Service s3Service;

    // -------------------------------------------------------------------------
    // UPLOAD
    // -------------------------------------------------------------------------

    @Transactional
    public DocumentoResponse upload(Long processoId, String tipoDocumento, MultipartFile arquivo) {
        validarArquivo(arquivo);

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        Usuario usuarioLogado = getUsuarioLogado();

        // Upload para S3 na pasta organizada por processo
        String pasta = "processos/" + processoId;
        S3Service.S3UploadResult resultado = s3Service.upload(arquivo, pasta);

        Documento documento = Documento.builder()
                .nome(arquivo.getOriginalFilename())
                .tipo(tipoDocumento.toUpperCase())
                .contentType(arquivo.getContentType())
                .urlS3(resultado.urlS3())
                .s3Key(resultado.s3Key())
                .tamanhoBytes(resultado.tamanhoBytes())
                .processo(processo)
                .enviadoPor(usuarioLogado)
                .build();

        Documento salvo = documentoRepository.save(documento);
        log.info("Documento salvo: id={}, tipo={}, processoId={}", salvo.getId(), tipoDocumento, processoId);

        String urlDownload = s3Service.gerarUrlPresignada(salvo.getS3Key());

        return DocumentoResponse.builder()
                .id(salvo.getId())
                .nome(salvo.getNome())
                .tipo(salvo.getTipo())
                .contentType(salvo.getContentType())
                .tamanhoBytes(salvo.getTamanhoBytes())
                .urlDownload(urlDownload)
                .enviadoEm(salvo.getEnviadoEm())
                .enviadoPor(salvo.getEnviadoPor().getNome())
                .build();
    }

    // -------------------------------------------------------------------------
    // LISTAR POR PROCESSO
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listarPorProcesso(Long processoId) {
        if (!processoRepository.existsById(processoId)) {
            throw new RecursoNaoEncontradoException("Processo", processoId);
        }
        return documentoRepository.findByProcessoId(processoId)
                .stream()
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
    }

    // -------------------------------------------------------------------------
    // DELETAR
    // -------------------------------------------------------------------------

    @Transactional
    public void deletar(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));

        s3Service.deletar(documento.getS3Key());
        documentoRepository.delete(documento);
        log.info("Documento deletado: id={}, s3Key={}", documentoId, documento.getS3Key());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Arquivo não pode ser vazio.");
        }
        if (!CONTENT_TYPES_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new RegraDeNegocioException(
                    "Tipo de arquivo não permitido: " + arquivo.getContentType() +
                    ". Permitidos: PDF, JPEG, PNG, TIFF.");
        }
        // 20MB
        if (arquivo.getSize() > 20 * 1024 * 1024) {
            throw new RegraDeNegocioException("Arquivo excede o tamanho máximo de 20MB.");
        }
    }

    private Usuario getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário logado não encontrado"));
    }
}
