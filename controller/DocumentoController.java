package br.gov.alvara.controller;

import br.gov.alvara.dto.response.DocumentoResponse;
import br.gov.alvara.service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    /**
     * POST /documentos/upload
     * Faz upload de um documento vinculado a um processo.
     *
     * Parâmetros multipart/form-data:
     *   - processoId: Long
     *   - tipoDocumento: String (ex: "CNPJ", "ALVARA_BOMBEIROS")
     *   - arquivo: MultipartFile (PDF, JPEG, PNG ou TIFF, max 20MB)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<DocumentoResponse> upload(
            @RequestParam Long processoId,
            @RequestParam String tipoDocumento,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(documentoService.upload(processoId, tipoDocumento, arquivo));
    }

    /**
     * GET /documentos/processo/{processoId}
     * Lista todos os documentos de um processo com URLs pré-assinadas para download.
     */
    @GetMapping("/processo/{processoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<List<DocumentoResponse>> listarPorProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(documentoService.listarPorProcesso(processoId));
    }

    /**
     * DELETE /documentos/{id}
     * Remove um documento do banco e do S3.
     * Restrito ao ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        documentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
