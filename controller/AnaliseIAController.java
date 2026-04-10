package br.gov.alvara.controller;

import br.gov.alvara.dto.response.AnaliseIAResponse;
import br.gov.alvara.service.AnaliseIAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analise")
@RequiredArgsConstructor
public class AnaliseIAController {

    private final AnaliseIAService analiseIAService;

    /**
     * POST /analise/{processoId}
     * Dispara a análise IA para o processo informado.
     * O processo deve ter documentos enviados.
     * Retorna o resultado da análise persistido.
     */
    @PostMapping("/{processoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<AnaliseIAResponse> analisar(@PathVariable Long processoId) {
        return ResponseEntity.ok(analiseIAService.analisarProcesso(processoId));
    }

    /**
     * GET /analise/{processoId}/ultima
     * Retorna a análise mais recente de um processo.
     */
    @GetMapping("/{processoId}/ultima")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVIDOR')")
    public ResponseEntity<AnaliseIAResponse> ultimaAnalise(@PathVariable Long processoId) {
        return ResponseEntity.ok(analiseIAService.buscarUltimaAnalise(processoId));
    }
}
