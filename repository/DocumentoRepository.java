package br.gov.alvara.repository;

import br.gov.alvara.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByProcessoId(Long processoId);
    boolean existsByProcessoIdAndTipo(Long processoId, String tipo);
}
