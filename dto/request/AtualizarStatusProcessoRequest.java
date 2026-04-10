package br.gov.alvara.dto.request;

import br.gov.alvara.enums.StatusProcesso;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtualizarStatusProcessoRequest {

    @NotNull(message = "Status obrigatório")
    private StatusProcesso status;

    private String observacoes;
}
