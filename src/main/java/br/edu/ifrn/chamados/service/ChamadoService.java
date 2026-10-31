package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.AssumirEntrada;
import br.edu.ifrn.chamados.dto.CancelarEntrada;
import br.edu.ifrn.chamados.dto.ChamadoDetalhe;
import br.edu.ifrn.chamados.dto.ChamadoEntrada;
import br.edu.ifrn.chamados.dto.ChamadoFiltro;
import br.edu.ifrn.chamados.dto.ChamadoResumo;
import br.edu.ifrn.chamados.dto.InteracaoEntrada;
import br.edu.ifrn.chamados.dto.InteracaoResposta;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.dto.ResolverEntrada;
import br.edu.ifrn.chamados.enums.Prioridade;
import br.edu.ifrn.chamados.enums.StatusChamado;
import br.edu.ifrn.chamados.enums.TipoUsuario;
import br.edu.ifrn.chamados.exception.RecursoNaoEncontradoException;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.Chamado;
import br.edu.ifrn.chamados.model.Interacao;
import br.edu.ifrn.chamados.model.Usuario;
import br.edu.ifrn.chamados.repository.ChamadoRepository;
import br.edu.ifrn.chamados.repository.InteracaoRepository;
import br.edu.ifrn.chamados.specification.ChamadoSpecification;
import br.edu.ifrn.chamados.specification.Filtros;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {
    private final ChamadoRepository chamados;
    private final InteracaoRepository interacoes;
    private final UsuarioService usuarios;
    private final CategoriaService categorias;

    private Chamado obter(Long id) {
        return chamados.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));
    }

    private Chamado obterParaAtualizar(Long id) {
        return chamados.buscarParaAtualizar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado"));
    }

    public ChamadoDetalhe buscar(Long id) {
        return ChamadoDetalhe.de(obter(id));
    }

    public PaginaResposta<ChamadoResumo> listar(ChamadoFiltro filtro, Pageable pagina) {
        return PaginaResposta.de(chamados.findAll(ChamadoSpecification.filtrar(filtro), paginaValida(pagina))
                .map(ChamadoResumo::de));
    }

    public PaginaResposta<ChamadoResumo> listarPorUsuario(Long usuarioId, StatusChamado status, Pageable pagina) {
        Usuario usuario = usuarios.obter(usuarioId);
        String vinculo = usuario.getTipo() == TipoUsuario.SOLICITANTE ? "solicitante.id" : "tecnico.id";
        return PaginaResposta.de(chamados.findAll(Filtros.<Chamado>igual(vinculo, usuarioId)
                .and(Filtros.igual("status", status)), paginaValida(pagina)).map(ChamadoResumo::de));
    }

    private Pageable paginaValida(Pageable pagina) {
        return Paginacao.validar(pagina, "id", "titulo", "descricao", "local", "prioridade", "status",
                "dataAbertura", "dataFechamento", "categoria.id", "solicitante.id", "tecnico.id");
    }

    @Transactional
    public ChamadoResumo abrir(ChamadoEntrada entrada) {
        Usuario solicitante = usuarios.obter(entrada.solicitanteId());
        if (solicitante.getTipo() != TipoUsuario.SOLICITANTE) {
            throw new RegraNegocioException("Somente solicitantes podem abrir chamados");
        }
        Chamado chamado = new Chamado();
        chamado.setTitulo(entrada.titulo().trim());
        chamado.setDescricao(entrada.descricao().trim());
        chamado.setLocal(entrada.local().trim());
        chamado.setPrioridade(entrada.prioridade() == null ? Prioridade.MEDIA : entrada.prioridade());
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categorias.obter(entrada.categoriaId()));
        return ChamadoResumo.de(chamados.saveAndFlush(chamado));
    }

    @Transactional
    public ChamadoResumo assumir(Long id, AssumirEntrada entrada) {
        Chamado chamado = obterParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.ABERTO);
        Usuario tecnico = usuarios.obter(entrada.tecnicoId());
        if (tecnico.getTipo() != TipoUsuario.TECNICO) {
            throw new RegraNegocioException("Somente técnicos podem assumir chamados");
        }
        if (tecnico.getSetor() == null
                || !tecnico.getSetor().getId().equals(chamado.getCategoria().getSetor().getId())) {
            throw new RegraNegocioException("Técnico não pertence ao setor responsável");
        }
        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        return ChamadoResumo.de(chamados.saveAndFlush(chamado));
    }

    @Transactional
    public ChamadoResumo resolver(Long id, ResolverEntrada entrada) {
        Chamado chamado = obterParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.EM_ATENDIMENTO);
        if (chamado.getTecnico() == null || !chamado.getTecnico().getId().equals(entrada.tecnicoId())) {
            throw new RegraNegocioException("Somente o técnico atribuído pode resolver o chamado");
        }
        if (entrada.solucao() == null || entrada.solucao().isBlank()) {
            throw new RegraNegocioException("A solução é obrigatória");
        }
        chamado.setSolucao(entrada.solucao().trim());
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDataFechamento(LocalDateTime.now());
        return ChamadoResumo.de(chamados.saveAndFlush(chamado));
    }

    @Transactional
    public ChamadoResumo cancelar(Long id, CancelarEntrada entrada) {
        Chamado chamado = obterParaAtualizar(id);
        exigirStatus(chamado, StatusChamado.ABERTO);
        if (!chamado.getSolicitante().getId().equals(entrada.solicitanteId())) {
            throw new RegraNegocioException("Somente o próprio solicitante pode cancelar o chamado");
        }
        chamado.setStatus(StatusChamado.CANCELADO);
        chamado.setDataFechamento(LocalDateTime.now());
        return ChamadoResumo.de(chamados.saveAndFlush(chamado));
    }

    @Transactional
    public InteracaoResposta interagir(Long id, InteracaoEntrada entrada) {
        Chamado chamado = obterParaAtualizar(id);
        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.CANCELADO) {
            throw new RegraNegocioException("Chamado finalizado não aceita interações");
        }
        boolean solicitante = chamado.getSolicitante().getId().equals(entrada.autorId());
        boolean tecnico = chamado.getTecnico() != null && chamado.getTecnico().getId().equals(entrada.autorId());
        if (!solicitante && !tecnico) {
            throw new RegraNegocioException("Somente o solicitante e o técnico atribuído podem interagir");
        }
        Interacao interacao = new Interacao();
        interacao.setTexto(entrada.texto().trim());
        interacao.setDataHora(LocalDateTime.now());
        interacao.setChamado(chamado);
        interacao.setAutor(usuarios.obter(entrada.autorId()));
        Interacao salva = interacoes.saveAndFlush(interacao);
        chamado.getInteracoes().add(salva);
        return InteracaoResposta.de(salva);
    }

    private void exigirStatus(Chamado chamado, StatusChamado esperado) {
        if (chamado.getStatus() != esperado) {
            throw new RegraNegocioException("Operação não permitida para chamado com status " + chamado.getStatus());
        }
    }
}
