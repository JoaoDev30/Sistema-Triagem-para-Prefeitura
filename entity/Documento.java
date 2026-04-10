package br.gov.alvara.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 100)
    private String tipo;       // Ex: "CNPJ", "ALVARA_BOMBEIROS", "LICENCA_SANITARIA"

    @Column(name = "content_type", length = 100)
    private String contentType; // Ex: "application/pdf"

    @Column(name = "url_s3", nullable = false, length = 1000)
    private String urlS3;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;      // Chave interna no bucket

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por_id", nullable = false)
    private Usuario enviadoPor;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private LocalDateTime enviadoEm;

    @PrePersist
    private void prePersist() {
        this.enviadoEm = LocalDateTime.now();
    }
}
