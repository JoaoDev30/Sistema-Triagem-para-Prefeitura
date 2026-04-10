package br.gov.alvara.controller;

import br.gov.alvara.dto.request.AtualizarStatusProcessoRequest;
import br.gov.alvara.dto.request.CriarProcessoRequest;
import br.gov.alvara.dto.response.ProcessoResponse;
import br.gov.alvara.enums.StatusProcesso;
import br.gov.alvara.service.ProcessoService;
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
@RequestMapping("/processos")
@RequiredArgsConstructor
public class ProcessoController {

    private final ProcessoService processoService;

    /**
     * POST /processos
     * Cria um novo processo de solicitação de alvará.
     * Disponível para ADMIN e SERVIDOR.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<ProcessoResponse> criar(@Valid @RequestBody CriarProcessoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(processoService.criar(request));
    }

    /**
     * GET /processos?status=PENDENTE&cnpj=00000000000000&nomeEmpresa=Farmacia&page=0&size=10
     * Lista processos com filtros opcionais e paginação.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<Page<ProcessoResponse>> listar(
            @RequestParam(required = false) StatusProcesso status,
            @RequestParam(required = false) String cnpj,
            @RequestParam(required = false) String nomeEmpresa,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        return ResponseEntity.ok(processoService.listar(status, cnpj, nomeEmpresa, pageable));
    }

    /**
     * GET /processos/{id}
     * Retorna processo completo com documentos e última análise IA.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<ProcessoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(processoService.buscarPorId(id));
    }

    /**
     * PATCH /processos/{id}/status
     * Atualiza o status do processo (ex: APROVADO, INDEFERIDO, EXIGENCIA).
     * Apenas ADMIN e SERVIDOR podem alterar status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<ProcessoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusProcessoRequest request) {
        return ResponseEntity.ok(processoService.atualizarStatus(id, request));
    }
}
