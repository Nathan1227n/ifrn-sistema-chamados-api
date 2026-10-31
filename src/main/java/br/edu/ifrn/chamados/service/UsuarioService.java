package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.dto.UsuarioEntrada;
import br.edu.ifrn.chamados.dto.UsuarioResposta;
import br.edu.ifrn.chamados.enums.TipoUsuario;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import br.edu.ifrn.chamados.specification.Filtros;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final ChamadoRepository chamados;
    private final SetorService setores;

    public Usuario obter(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    public UsuarioResposta buscar(Long id) {
        return UsuarioResposta.de(obter(id));
    }

    public PaginaResposta<UsuarioResposta> listar(String nome, TipoUsuario tipo, Long setorId, Pageable pagina) {
        return PaginaResposta.de(usuarios.findAll(Filtros.<Usuario>nomeContem(nome)
                .and(Filtros.igual("tipo", tipo)).and(Filtros.igual("setor.id", setorId)),
                Paginacao.validar(pagina, "id", "nome", "email", "matricula", "tipo", "setor.id"))
                .map(UsuarioResposta::de));
    }

    @Transactional
    public UsuarioResposta criar(UsuarioEntrada entrada) {
        return salvar(new Usuario(), entrada);
    }

    @Transactional
    public UsuarioResposta atualizar(Long id, UsuarioEntrada entrada) {
        Usuario usuario = obter(id);
        Long setorAtual = usuario.getSetor() == null ? null : usuario.getSetor().getId();
        if ((usuario.getTipo() != entrada.tipo() || !Objects.equals(setorAtual, entrada.setorId()))
                && chamados.existsBySolicitanteIdOrTecnicoId(id, id)) {
            throw new RegraNegocioException("Não é permitido mudar tipo ou setor de usuário com chamados vinculados");
        }
        return salvar(usuario, entrada);
    }

    private UsuarioResposta salvar(Usuario usuario, UsuarioEntrada entrada) {
        String email = entrada.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId() == null ? 0L : usuario.getId())) {
            throw new RegraNegocioException("Email já cadastrado");
        }
        if (entrada.tipo() == TipoUsuario.TECNICO && entrada.setorId() == null) {
            throw new RegraNegocioException("Técnico deve pertencer a um setor");
        }
        usuario.setNome(entrada.nome().trim());
        usuario.setEmail(email);
        usuario.setMatricula(entrada.matricula().trim());
        usuario.setTipo(entrada.tipo());
        usuario.setSetor(entrada.setorId() == null ? null : setores.obter(entrada.setorId()));
        return UsuarioResposta.de(usuarios.saveAndFlush(usuario));
    }
}
