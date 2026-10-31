package br.edu.ifrn.chamados.service;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.enums.TipoUsuario;
import br.edu.ifrn.chamados.exception.RegraNegocioException;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastrosServiceTest {
    @Mock SetorRepository setores;
    @Mock CategoriaRepository categorias;
    @Mock UsuarioRepository usuarios;
    @Mock ChamadoRepository chamados;
    private SetorService setorService;
    private CategoriaService categoriaService;
    private UsuarioService usuarioService;

    @BeforeEach
    void preparar() {
        setorService = new SetorService(setores, categorias, usuarios, chamados);
        categoriaService = new CategoriaService(categorias, chamados, setorService);
        usuarioService = new UsuarioService(usuarios, chamados, setorService);
    }

    @Test
    void emailDuplicadoEhRejeitadoSemDiferenciarMaiusculas() {
        when(usuarios.existsByEmailIgnoreCaseAndIdNot("ana@ifrn.edu.br", 0L)).thenReturn(true);
        assertThatThrownBy(() -> usuarioService.criar(new UsuarioEntrada("Ana", "ANA@ifrn.edu.br",
                "123", TipoUsuario.SOLICITANTE, null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Email");
        verify(usuarios, never()).saveAndFlush(any());
    }

    @Test
    void tecnicoPrecisaDeSetor() {
        assertThatThrownBy(() -> usuarioService.criar(new UsuarioEntrada("Carlos", "c@ifrn.edu.br",
                "123", TipoUsuario.TECNICO, null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("setor");
    }

    @Test
    void categoriaComChamadosNaoPodeSerExcluida() {
        when(categorias.findById(1L)).thenReturn(Optional.of(new Categoria()));
        when(chamados.existsByCategoriaId(1L)).thenReturn(true);
        assertThatThrownBy(() -> categoriaService.excluir(1L)).isInstanceOf(RegraNegocioException.class);
        verify(categorias, never()).delete(any(Categoria.class));
    }

    @Test
    void setorComChamadosNaoPodeSerExcluido() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(chamados.existsByCategoriaSetorId(1L)).thenReturn(true);
        assertThatThrownBy(() -> setorService.excluir(1L)).isInstanceOf(RegraNegocioException.class);
        verify(setores, never()).delete(any(Setor.class));
    }

    @Test
    void setorComCategoriasNaoPodeSerExcluido() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(categorias.existsBySetorId(1L)).thenReturn(true);
        assertThatThrownBy(() -> setorService.excluir(1L)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void setorComUsuariosNaoPodeSerExcluido() {
        when(setores.findById(1L)).thenReturn(Optional.of(new Setor()));
        when(usuarios.existsBySetorId(1L)).thenReturn(true);
        assertThatThrownBy(() -> setorService.excluir(1L)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void mudarSetorDeCategoriaComChamadosEhRejeitado() {
        Setor setor = new Setor();
        setor.setId(1L);
        Categoria categoria = new Categoria();
        categoria.setSetor(setor);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(chamados.existsByCategoriaId(1L)).thenReturn(true);
        assertThatThrownBy(() -> categoriaService.atualizar(1L, new CategoriaEntrada("Redes", null, 2L)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void mudarTipoDeUsuarioComChamadosEhRejeitado() {
        Usuario usuario = new Usuario();
        usuario.setTipo(TipoUsuario.SOLICITANTE);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(chamados.existsBySolicitanteIdOrTecnicoId(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> usuarioService.atualizar(1L, new UsuarioEntrada("Ana", "ana@ifrn.edu.br",
                "123", TipoUsuario.TECNICO, 1L))).isInstanceOf(RegraNegocioException.class);
    }
}
