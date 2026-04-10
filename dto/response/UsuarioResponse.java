package br.gov.alvara.dto.response;

import br.gov.alvara.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String nome;
    private String email;
    private Role role;
    private boolean ativo;
    private LocalDateTime criadoEm;
}
