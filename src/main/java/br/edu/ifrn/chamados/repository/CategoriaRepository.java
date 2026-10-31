package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {
    boolean existsBySetorId(Long setorId);
}
