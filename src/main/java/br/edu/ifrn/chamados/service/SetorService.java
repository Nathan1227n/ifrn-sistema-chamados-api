package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.CategoriaResposta;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.dto.SetorEntrada;
import br.edu.ifrn.chamados.dto.SetorResposta;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Setor;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.SetorRepository;
import br.edu.ifrn.chamados.repository.UsuarioRepository;
import br.edu.ifrn.chamados.specification.Filtros;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SetorService {
    private final SetorRepository setores;
    private final CategoriaRepository categorias;
    private final UsuarioRepository usuarios;
    private final ChamadoRepository chamados;

    public Setor obter(Long id) {
        return setores.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado"));
    }

    public SetorResposta buscar(Long id) {
        return SetorResposta.de(obter(id));
    }

    public PaginaResposta<SetorResposta> listar(String nome, Pageable pagina) {
        return PaginaResposta.de(setores.findAll(Filtros.nomeContem(nome),
                Paginacao.validar(pagina, "id", "nome", "sigla")).map(SetorResposta::de));
    }

    public PaginaResposta<CategoriaResposta> listarCategorias(Long id, Pageable pagina) {
        obter(id);
        return PaginaResposta.de(categorias.findAll(Filtros.igual("setor.id", id),
                Paginacao.validar(pagina, "id", "nome", "descricao", "setor.id")).map(CategoriaResposta::de));
    }

    @Transactional
    public SetorResposta criar(SetorEntrada entrada) {
        return salvar(new Setor(), entrada);
    }

    @Transactional
    public SetorResposta atualizar(Long id, SetorEntrada entrada) {
        return salvar(obter(id), entrada);
    }

    private SetorResposta salvar(Setor setor, SetorEntrada entrada) {
        String sigla = entrada.sigla().trim().toUpperCase(Locale.ROOT);
        if (setores.existsBySiglaIgnoreCaseAndIdNot(sigla, setor.getId() == null ? 0L : setor.getId())) {
            throw new RegraNegocioException("Sigla já cadastrada");
        }
        setor.setNome(entrada.nome().trim());
        setor.setSigla(sigla);
        return SetorResposta.de(setores.saveAndFlush(setor));
    }

    @Transactional
    public void excluir(Long id) {
        Setor setor = obter(id);
        if (chamados.existsByCategoriaSetorId(id)) {
            throw new RegraNegocioException("Setor possui chamados vinculados");
        }
        if (categorias.existsBySetorId(id) || usuarios.existsBySetorId(id)) {
            throw new RegraNegocioException("Setor possui categorias ou usuários vinculados");
        }
        setores.delete(setor);
        setores.flush();
    }
}
