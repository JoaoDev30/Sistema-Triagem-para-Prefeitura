package br.gov.alvara.entity;

import br.gov.alvara.enums.StatusAnalise;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "analises_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "resultado_json", columnDefinition = "TEXT")
    private String resultadoJson;   // JSON completo retornado pela IA

    @Column(name = "prompt_enviado", columnDefinition = "TEXT")
    private String promptEnviado;   // Auditoria: guarda o prompt exato

    @Enumerated(EnumType.STRING)
    @Column(name = "status_geral", nullable = false)
    @Builder.Default
    private StatusAnalise statusGeral = StatusAnalise.PENDENTE;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;    // Preenchido apenas em caso de falha

    @Column(name = "data_analise", nullable = false, updatable = false)
    private LocalDateTime dataAnalise;

    @Column(name = "modelo_utilizado", length = 100)
    private String modeloUtilizado; // Ex: "gpt-4o"

    @PrePersist
    private void prePersist() {
        this.dataAnalise = LocalDateTime.now();
    }
}
