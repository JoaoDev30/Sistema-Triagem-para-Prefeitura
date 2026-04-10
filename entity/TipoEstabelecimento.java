package br.gov.alvara.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "tipos_estabelecimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoEstabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @OneToMany(mappedBy = "tipoEstabelecimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegraDocumento> regrasDocumento;

    @OneToMany(mappedBy = "tipoEstabelecimento")
    private List<Processo> processos;
}
