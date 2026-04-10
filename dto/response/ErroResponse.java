package br.gov.alvara.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErroResponse {
    private int status;
    private String erro;
    private String mensagem;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, String> campos;  // Erros de validação por campo
}
