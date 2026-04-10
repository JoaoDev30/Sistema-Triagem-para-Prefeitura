package br.gov.alvara.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CriarProcessoRequest {

    @NotBlank(message = "Nome da empresa obrigatório")
    @Size(max = 200)
    private String nomeEmpresa;

    @NotBlank(message = "CNPJ obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos numéricos")
    private String cnpj;

    @NotNull(message = "Tipo de estabelecimento obrigatório")
    private Long tipoEstabelecimentoId;

    @Size(max = 2000)
    private String observacoes;
}
