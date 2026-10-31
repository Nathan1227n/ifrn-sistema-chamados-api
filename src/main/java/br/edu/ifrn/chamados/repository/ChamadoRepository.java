package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Chamado;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
    boolean existsByCategoriaId(Long categoriaId);
    boolean existsByCategoriaSetorId(Long setorId);
    boolean existsBySolicitanteIdOrTecnicoId(Long solicitanteId, Long tecnicoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Chamado c where c.id = :id")
    Optional<Chamado> buscarParaAtualizar(@Param("id") Long id);
}
