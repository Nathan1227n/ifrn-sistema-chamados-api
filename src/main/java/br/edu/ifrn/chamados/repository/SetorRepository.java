package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SetorRepository extends JpaRepository<Setor, Long>, JpaSpecificationExecutor<Setor> {
    boolean existsBySiglaIgnoreCaseAndIdNot(String sigla, Long id);
}
