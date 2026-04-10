package br.gov.alvara.repository;

import br.gov.alvara.entity.AnaliseIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseIARepository extends JpaRepository<AnaliseIA, Long> {
    List<AnaliseIA> findByProcessoIdOrderByDataAnaliseDesc(Long processoId);
    Optional<AnaliseIA> findTopByProcessoIdOrderByDataAnaliseDesc(Long processoId);
}
