package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.CategoriaEntrada;
import br.edu.ifrn.chamados.dto.CategoriaResposta;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Categoria;
import br.edu.ifrn.chamados.repository.CategoriaRepository;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.specification.Filtros;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository categorias;
    private final ChamadoRepository chamados;
    private final SetorService setores;

    public Categoria obter(Long id) {
        return categorias.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    public CategoriaResposta buscar(Long id) {
        return CategoriaResposta.de(obter(id));
    }

    public PaginaResposta<CategoriaResposta> listar(String nome, Long setorId, Pageable pagina) {
        return PaginaResposta.de(categorias.findAll(Filtros.<Categoria>nomeContem(nome)
                .and(Filtros.igual("setor.id", setorId)),
                Paginacao.validar(pagina, "id", "nome", "descricao", "setor.id")).map(CategoriaResposta::de));
    }

    @Transactional
    public CategoriaResposta criar(CategoriaEntrada entrada) {
        return salvar(new Categoria(), entrada);
    }

    @Transactional
    public CategoriaResposta atualizar(Long id, CategoriaEntrada entrada) {
        Categoria categoria = obter(id);
        if (!categoria.getSetor().getId().equals(entrada.setorId()) && chamados.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Não é permitido mudar o setor de categoria com chamados vinculados");
        }
        return salvar(categoria, entrada);
    }

    private CategoriaResposta salvar(Categoria categoria, CategoriaEntrada entrada) {
        categoria.setSetor(setores.obter(entrada.setorId()));
        categoria.setNome(entrada.nome().trim());
        categoria.setDescricao(entrada.descricao());
        return CategoriaResposta.de(categorias.saveAndFlush(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = obter(id);
        if (chamados.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Categoria possui chamados vinculados");
        }
        categorias.delete(categoria);
        categorias.flush();
    }
}
