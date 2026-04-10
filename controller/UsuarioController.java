package br.gov.alvara.controller;

import br.gov.alvara.dto.request.CriarUsuarioRequest;
import br.gov.alvara.dto.response.UsuarioResponse;
import br.gov.alvara.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // Classe inteira restrita ao ADMIN
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * POST /usuarios
     * Cria um novo usuário (ADMIN ou SERVIDOR).
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(request));
    }

    /**
     * GET /usuarios?page=0&size=20
     * Lista todos os usuários paginados.
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(pageable));
    }

    /**
     * GET /usuarios/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    /**
     * PATCH /usuarios/{id}/desativar
     * Desativa o usuário (soft delete).
     */
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /usuarios/{id}/ativar
     * Reativa um usuário previamente desativado.
     */
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
