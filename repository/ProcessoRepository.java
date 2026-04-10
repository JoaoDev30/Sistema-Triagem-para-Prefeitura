package br.gov.alvara.repository;

import br.gov.alvara.entity.Processo;
import br.gov.alvara.enums.StatusProcesso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    Page<Processo> findAllByOrderByDataCriacaoDesc(Pageable pageable);

    Page<Processo> findByStatus(StatusProcesso status, Pageable pageable);

    Page<Processo> findByCriadoPorId(Long usuarioId, Pageable pageable);

    @Query("SELECT p FROM Processo p WHERE " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:cnpj IS NULL OR p.cnpj = :cnpj) AND " +
           "(:nomeEmpresa IS NULL OR LOWER(p.nomeEmpresa) LIKE LOWER(CONCAT('%', :nomeEmpresa, '%')))")
    Page<Processo> buscarComFiltros(
            @Param("status") StatusProcesso status,
            @Param("cnpj") String cnpj,
            @Param("nomeEmpresa") String nomeEmpresa,
            Pageable pageable
    );

    List<Processo> findByCnpj(String cnpj);
}
