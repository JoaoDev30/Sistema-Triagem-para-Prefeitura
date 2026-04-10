package br.gov.alvara.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoResponse {
    private Long id;
    private String nome;
    private String tipo;
    private String contentType;
    private Long tamanhoBytes;
    private String urlDownload;   // URL pré-assinada do S3
    private LocalDateTime enviadoEm;
    private String enviadoPor;
}
