package br.gov.alvara.controller;

import br.gov.alvara.entity.RegraDocumento;
import br.gov.alvara.entity.TipoEstabelecimento;
import br.gov.alvara.exception.RecursoNaoEncontradoException;
import br.gov.alvara.repository.RegraDocumentoRepository;
import br.gov.alvara.repository.TipoEstabelecimentoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ─────────────────────────────────────────────────────────────────────────────
// TIPOS DE ESTABELECIMENTO
// ─────────────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/tipos-estabelecimento")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class TipoEstabelecimentoController {

    private final TipoEstabelecimentoRepository repository;

    @GetMapping
    public ResponseEntity<List<TipoEstabelecimento>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<TipoEstabelecimento> criar(@Valid @RequestBody TipoEstabelecimentoRequest req) {
        TipoEstabelecimento tipo = TipoEstabelecimento.builder()
                .nome(req.getNome())
                .descricao(req.getDescricao())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(tipo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        TipoEstabelecimento tipo = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TipoEstabelecimento", id));
        repository.delete(tipo);
        return ResponseEntity.noContent().build();
    }

    @Data
    static class TipoEstabelecimentoRequest {
        @NotBlank private String nome;
        private String descricao;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REGRAS DE DOCUMENTO
// ─────────────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/regras-documento")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class RegraDocumentoController {

    private final RegraDocumentoRepository regraRepository;
    private final TipoEstabelecimentoRepository tipoRepository;

    @GetMapping("/tipo/{tipoId}")
    public ResponseEntity<List<RegraDocumento>> listarPorTipo(@PathVariable Long tipoId) {
        return ResponseEntity.ok(regraRepository.findByTipoEstabelecimentoId(tipoId));
    }

    @PostMapping
    public ResponseEntity<RegraDocumento> criar(@Valid @RequestBody RegraDocumentoRequest req) {
        TipoEstabelecimento tipo = tipoRepository.findById(req.getTipoEstabelecimentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("TipoEstabelecimento", req.getTipoEstabelecimentoId()));

        RegraDocumento regra = RegraDocumento.builder()
                .tipoEstabelecimento(tipo)
                .nomeDocumento(req.getNomeDocumento())
                .obrigatorio(req.isObrigatorio())
                .descricao(req.getDescricao())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(regraRepository.save(regra));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        RegraDocumento regra = regraRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("RegraDocumento", id));
        regraRepository.delete(regra);
        return ResponseEntity.noContent().build();
    }

    @Data
    static class RegraDocumentoRequest {
        @NotNull  private Long tipoEstabelecimentoId;
        @NotBlank private String nomeDocumento;
        private boolean obrigatorio = true;
        private String descricao;
    }
}
