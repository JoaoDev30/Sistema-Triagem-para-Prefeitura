package br.gov.alvara.dto.response;

import br.gov.alvara.enums.StatusAnalise;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnaliseIAResponse {
    private Long id;
    private Long processoId;
    private StatusAnalise statusGeral;
    private Object resultadoJson;   // Desserializado para exibição
    private String modeloUtilizado;
    private LocalDateTime dataAnalise;
    private String mensagemErro;
}
