package br.gov.alvara.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regras_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegraDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_estabelecimento_id", nullable = false)
    private TipoEstabelecimento tipoEstabelecimento;

    @Column(name = "nome_documento", nullable = false, length = 200)
    private String nomeDocumento;

    @Column(nullable = false)
    private boolean obrigatorio = true;

    @Column(length = 500)
    private String descricao;
}
