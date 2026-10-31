package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.enums.*;
import br.edu.ifrn.chamados.exception.*;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {
    @Mock ChamadoRepository chamados;
    @Mock InteracaoRepository interacoes;
    @Mock UsuarioService usuarios;
    @Mock CategoriaService categorias;
    @InjectMocks ChamadoService servico;
    private Usuario solicitante;
    private Usuario tecnico;
    private Categoria categoria;
    private Chamado chamado;

    @BeforeEach
    void preparar() {
        Setor setor = new Setor();
        setor.setId(1L);
        setor.setNome("COTIC");
        setor.setSigla("COTIC");
        solicitante = usuario(1L, TipoUsuario.SOLICITANTE, null);
        tecnico = usuario(3L, TipoUsuario.TECNICO, setor);
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Redes");
        categoria.setSetor(setor);
        chamado = new Chamado();
        chamado.setId(1L);
        chamado.setSolicitante(solicitante);
        chamado.setCategoria(categoria);
        chamado.setStatus(StatusChamado.ABERTO);
    }

    private Usuario usuario(Long id, TipoUsuario tipo, Setor setor) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipo(tipo);
        usuario.setSetor(setor);
        return usuario;
    }

    private void prepararAlteracao() {
        when(chamados.buscarParaAtualizar(1L)).thenReturn(Optional.of(chamado));
    }

    private void permitirSalvar() {
        when(chamados.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void solicitanteAbreChamadoComValoresPadrao() {
        when(usuarios.obter(1L)).thenReturn(solicitante);
        when(categorias.obter(1L)).thenReturn(categoria);
        permitirSalvar();
        LocalDateTime antes = LocalDateTime.now();
        ChamadoResumo resposta = servico.abrir(new ChamadoEntrada("Internet", "Sem conexão", "Sala 1", null, 1L, 1L));
        assertThat(resposta.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(resposta.prioridade()).isEqualTo(Prioridade.MEDIA);
        assertThat(resposta.dataAbertura()).isBetween(antes, LocalDateTime.now());
        assertThat(resposta.tecnico()).isNull();
        assertThat(resposta.dataFechamento()).isNull();
    }

    @Test
    void preservaPrioridadeInformada() {
        when(usuarios.obter(1L)).thenReturn(solicitante);
        when(categorias.obter(1L)).thenReturn(categoria);
        permitirSalvar();
        assertThat(servico.abrir(new ChamadoEntrada("Internet", "Sem conexão", "Sala 1", Prioridade.ALTA, 1L, 1L))
                .prioridade()).isEqualTo(Prioridade.ALTA);
    }

    @Test
    void tecnicoNaoAbreChamado() {
        when(usuarios.obter(3L)).thenReturn(tecnico);
        assertThatThrownBy(() -> servico.abrir(new ChamadoEntrada("A", "B", "C", null, 1L, 3L)))
                .isInstanceOf(RegraNegocioException.class);
        verify(chamados, never()).saveAndFlush(any());
    }

    @Test
    void tecnicoDoSetorAssumeChamado() {
        prepararAlteracao();
        permitirSalvar();
        when(usuarios.obter(3L)).thenReturn(tecnico);
        ChamadoResumo resposta = servico.assumir(1L, new AssumirEntrada(3L));
        assertThat(resposta.status()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        assertThat(resposta.tecnico().id()).isEqualTo(3L);
    }

    @Test
    void tecnicoDeOutroSetorNaoAssume() {
        prepararAlteracao();
        Setor outro = new Setor();
        outro.setId(2L);
        tecnico.setSetor(outro);
        when(usuarios.obter(3L)).thenReturn(tecnico);
        assertThatThrownBy(() -> servico.assumir(1L, new AssumirEntrada(3L)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("setor");
    }

    @Test
    void solicitanteNaoAssume() {
        prepararAlteracao();
        when(usuarios.obter(1L)).thenReturn(solicitante);
        assertThatThrownBy(() -> servico.assumir(1L, new AssumirEntrada(1L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"EM_ATENDIMENTO", "RESOLVIDO", "CANCELADO"})
    void naoAssumeChamadoForaDoStatusAberto(StatusChamado status) {
        prepararAlteracao();
        chamado.setStatus(status);
        assertThatThrownBy(() -> servico.assumir(1L, new AssumirEntrada(3L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void tecnicoAtribuidoResolveComSolucao() {
        prepararAlteracao();
        permitirSalvar();
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);
        ChamadoResumo resposta = servico.resolver(1L, new ResolverEntrada(3L, " Conexão reparada "));
        assertThat(resposta.status()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(resposta.solucao()).isEqualTo("Conexão reparada");
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejeitaResolucaoSemSolucao(String solucao) {
        prepararAlteracao();
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);
        assertThatThrownBy(() -> servico.resolver(1L, new ResolverEntrada(3L, solucao)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void tecnicoDiferenteNaoResolve() {
        prepararAlteracao();
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setTecnico(tecnico);
        assertThatThrownBy(() -> servico.resolver(1L, new ResolverEntrada(4L, "Reparado")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"ABERTO", "RESOLVIDO", "CANCELADO"})
    void naoResolveForaDeAtendimento(StatusChamado status) {
        prepararAlteracao();
        chamado.setStatus(status);
        assertThatThrownBy(() -> servico.resolver(1L, new ResolverEntrada(3L, "Reparado")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void solicitanteCancelaAberto() {
        prepararAlteracao();
        permitirSalvar();
        ChamadoResumo resposta = servico.cancelar(1L, new CancelarEntrada(1L));
        assertThat(resposta.status()).isEqualTo(StatusChamado.CANCELADO);
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @Test
    void outroUsuarioNaoCancela() {
        prepararAlteracao();
        assertThatThrownBy(() -> servico.cancelar(1L, new CancelarEntrada(2L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"EM_ATENDIMENTO", "RESOLVIDO", "CANCELADO"})
    void naoCancelaForaDoStatusAberto(StatusChamado status) {
        prepararAlteracao();
        chamado.setStatus(status);
        assertThatThrownBy(() -> servico.cancelar(1L, new CancelarEntrada(1L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"RESOLVIDO", "CANCELADO"})
    void finalizadoNaoAceitaInteracoes(StatusChamado status) {
        prepararAlteracao();
        chamado.setStatus(status);
        assertThatThrownBy(() -> servico.interagir(1L, new InteracaoEntrada(1L, "Texto")))
                .isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(interacoes);
    }

    @Test
    void usuarioSemVinculoNaoInterage() {
        prepararAlteracao();
        assertThatThrownBy(() -> servico.interagir(1L, new InteracaoEntrada(2L, "Texto")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void tecnicoAindaNaoAtribuidoNaoInterage() {
        prepararAlteracao();
        assertThatThrownBy(() -> servico.interagir(1L, new InteracaoEntrada(3L, "Texto")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 3L})
    void participantesInteragem(Long autorId) {
        prepararAlteracao();
        chamado.setTecnico(tecnico);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        when(usuarios.obter(autorId)).thenReturn(autorId == 1L ? solicitante : tecnico);
        when(interacoes.saveAndFlush(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        InteracaoResposta resposta = servico.interagir(1L, new InteracaoEntrada(autorId, "Texto"));
        assertThat(resposta.autor().id()).isEqualTo(autorId);
        assertThat(resposta.dataHora()).isNotNull();
    }

    @Test
    void chamadoInexistenteRetornaExcecaoPropria() {
        when(chamados.buscarParaAtualizar(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.cancelar(9L, new CancelarEntrada(1L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
