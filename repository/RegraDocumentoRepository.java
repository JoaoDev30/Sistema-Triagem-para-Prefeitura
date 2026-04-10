package br.gov.alvara.repository;

import br.gov.alvara.entity.RegraDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraDocumentoRepository extends JpaRepository<RegraDocumento, Long> {
    List<RegraDocumento> findByTipoEstabelecimentoId(Long tipoEstabelecimentoId);
    List<RegraDocumento> findByTipoEstabelecimentoIdAndObrigatorio(Long tipoEstabelecimentoId, boolean obrigatorio);
}
