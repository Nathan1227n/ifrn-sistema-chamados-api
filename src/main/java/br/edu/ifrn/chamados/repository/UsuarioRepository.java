package br.edu.ifrn.chamados.repository;

import br.edu.ifrn.chamados.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsBySetorId(Long setorId);
}
