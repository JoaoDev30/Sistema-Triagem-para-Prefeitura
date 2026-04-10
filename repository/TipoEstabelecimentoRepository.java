package br.gov.alvara.repository;

import br.gov.alvara.entity.TipoEstabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoEstabelecimentoRepository extends JpaRepository<TipoEstabelecimento, Long> {
    Optional<TipoEstabelecimento> findByNome(String nome);
}
