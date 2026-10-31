package br.edu.ifrn.chamados;

import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.enums.*;
import br.edu.ifrn.chamados.model.*;
import br.edu.ifrn.chamados.repository.*;
import br.edu.ifrn.chamados.service.ChamadoService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integracao")
@Transactional
class ApiPersistenciaIT {
    @Autowired MockMvc mvc;
    @Autowired ChamadoService servico;
    @Autowired ChamadoRepository chamados;
    @Autowired jakarta.persistence.EntityManager entidadeManager;
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    private Long abrir(Long solicitante, Long categoria, Prioridade prioridade) {
        return servico.abrir(new ChamadoEntrada("Projetor não funciona", "Não liga", "Sala 12",
                prioridade, categoria, solicitante)).id();
    }

    @Test
    void migrationsValidasComDadosIniciaisEEntidadesCompativeis() {
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from setores", Integer.class)).isGreaterThanOrEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from categorias", Integer.class)).isGreaterThanOrEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from usuarios", Integer.class)).isGreaterThanOrEqualTo(4);
    }

    @Test
    void fluxoCompletoComHistoricoCronologico() throws Exception {
        Long id = abrir(1L, 2L, null);
        mvc.perform(patch("/chamados/{id}/assumir", id).contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
        mvc.perform(post("/chamados/{id}/interacoes", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"autorId\":1,\"texto\":\"Ainda não liga\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.autor.id").value(1));
        mvc.perform(post("/chamados/{id}/interacoes", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"autorId\":3,\"texto\":\"Iniciando reparo\"}"))
                .andExpect(status().isCreated());
        mvc.perform(patch("/chamados/{id}/resolver", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":3,\"solucao\":\"Fonte substituída\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.dataFechamento").exists());
        entidadeManager.clear();
        mvc.perform(get("/chamados/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.chamado.solucao").value("Fonte substituída"))
                .andExpect(jsonPath("$.interacoes.length()").value(2))
                .andExpect(jsonPath("$.interacoes[0].texto").value("Ainda não liga"))
                .andExpect(jsonPath("$.interacoes[1].autor.id").value(3))
                .andExpect(jsonPath("$.interacoes[0].dataHora").exists());
        mvc.perform(post("/chamados/{id}/interacoes", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"autorId\":1,\"texto\":\"Novo texto\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aberturaRetorna201ELocation() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Projetor", "descricao":"Não liga", "local":"Sala 12", "categoriaId":2, "solicitanteId":1}
                """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ABERTO")).andExpect(jsonPath("$.prioridade").value("MEDIA"))
                .andExpect(jsonPath("$.tecnico").isEmpty());
    }

    @Test
    void filtrosCombinadosEPaginacaoDeChamados() throws Exception {
        Long primeiro = abrir(1L, 1L, Prioridade.ALTA);
        Long segundo = abrir(1L, 1L, Prioridade.ALTA);
        abrir(2L, 3L, Prioridade.BAIXA);
        mvc.perform(get("/chamados?status=ABERTO&setorId=1&prioridade=ALTA&categoriaId=1&solicitanteId=1&page=0&size=1&sort=id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(segundo))
                .andExpect(jsonPath("$.numeroPagina").value(0)).andExpect(jsonPath("$.tamanhoPagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/chamados?status=ABERTO&setorId=1&prioridade=ALTA&page=1&size=1&sort=id,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(primeiro));
    }

    @Test
    void filtroPorTecnicoEPorUsuario() throws Exception {
        Long id = abrir(1L, 1L, Prioridade.ALTA);
        abrir(2L, 1L, null);
        servico.assumir(id, new AssumirEntrada(3L));
        mvc.perform(get("/chamados?tecnicoId=3&status=EM_ATENDIMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/usuarios/1/chamados?status=EM_ATENDIMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(id))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/usuarios/3/chamados?status=EM_ATENDIMENTO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(id));
        mvc.perform(get("/usuarios/4/chamados")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void intervaloIncluiTodoODiaFinal() throws Exception {
        Long id = abrir(1L, 1L, null);
        Chamado chamado = chamados.findById(id).orElseThrow();
        chamado.setDataAbertura(LocalDateTime.of(2026, 10, 30, 23, 59, 59, 999000000));
        chamados.saveAndFlush(chamado);
        Long outroId = abrir(1L, 1L, null);
        Chamado outro = chamados.findById(outroId).orElseThrow();
        outro.setDataAbertura(LocalDateTime.of(2026, 10, 31, 0, 0));
        chamados.saveAndFlush(outro);
        mvc.perform(get("/chamados?dataInicio=2026-10-30&dataFim=2026-10-30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id));
    }

    @Test
    void rejeitaDatasInvalidasEOrdenacaoDesconhecida() throws Exception {
        for (String consulta : new String[]{"dataInicio=30-10-2026", "dataFim=2026-02-30",
                "dataInicio=2026-10-31&dataFim=2026-10-30", "sort=inexistente", "categoriaId=-1"}) {
            mvc.perform(get("/chamados?" + consulta)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void categoriasESetoresFiltramNomeSemDiferenciarMaiusculas() throws Exception {
        mvc.perform(get("/setores?nome=MANUTEN&page=0&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].sigla").value("MANUT"));
        mvc.perform(get("/categorias?nome=EQUIPAMENTOS&setorId=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/setores/1/categorias?size=1&page=1&sort=id"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(2))
                .andExpect(jsonPath("$.totalElementos").value(2));
    }

    @Test
    void usuariosCombinamNomeTipoESetor() throws Exception {
        mvc.perform(get("/usuarios?nome=CARLOS&tipo=TECNICO&setorId=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(3));
    }

    @Test
    void limitesEPadroesDePaginacao() throws Exception {
        mvc.perform(get("/setores")).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroPagina").value(0)).andExpect(jsonPath("$.tamanhoPagina").value(10));
        mvc.perform(get("/usuarios?size=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanhoPagina").value(50));
        mvc.perform(get("/categorias?page=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0));
    }

    @Test
    void emailDuplicadoETecnicoSemSetorRetornam409() throws Exception {
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Outra Ana", "email":"ANA@ifrn.edu.br", "matricula":"456", "tipo":"SOLICITANTE"}
                """))
                .andExpect(status().isConflict());
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Técnico", "email":"novo@ifrn.edu.br", "matricula":"456", "tipo":"TECNICO"}
                """))
                .andExpect(status().isConflict());
    }

    @Test
    void exclusoesProtegemReferencias() throws Exception {
        abrir(1L, 1L, null);
        mvc.perform(delete("/categorias/1")).andExpect(status().isConflict());
        mvc.perform(delete("/setores/1")).andExpect(status().isConflict());
        mvc.perform(delete("/setores/2")).andExpect(status().isConflict());
        mvc.perform(delete("/categorias/4")).andExpect(status().isNoContent());
        mvc.perform(get("/categorias/4")).andExpect(status().isNotFound());
    }

    @Test
    void crudDeCadastros() throws Exception {
        mvc.perform(post("/setores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Biblioteca\",\"sigla\":\"BIB\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"));
        Long id = jdbc.queryForObject("select id from setores where sigla='BIB'", Long.class);
        mvc.perform(get("/setores/{id}", id)).andExpect(status().isOk());
        mvc.perform(put("/setores/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Biblioteca Central\",\"sigla\":\"BIB\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Acervo\",\"setorId\":" + id + "}"))
                .andExpect(status().isCreated());
        Long categoriaId = jdbc.queryForObject("select id from categorias where nome='Acervo'", Long.class);
        mvc.perform(get("/categorias/{id}", categoriaId)).andExpect(status().isOk());
        mvc.perform(put("/categorias/{id}", categoriaId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Acervo Geral\",\"descricao\":\"Livros\",\"setorId\":" + id + "}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/categorias/{id}", categoriaId)).andExpect(status().isNoContent());
        mvc.perform(delete("/setores/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Eva", "email":"eva@ifrn.edu.br", "matricula":"999", "tipo":"SOLICITANTE"}
                """))
                .andExpect(status().isCreated());
        Long usuarioId = jdbc.queryForObject("select id from usuarios where email='eva@ifrn.edu.br'", Long.class);
        mvc.perform(get("/usuarios/{id}", usuarioId)).andExpect(status().isOk());
        mvc.perform(put("/usuarios/{id}", usuarioId).contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Eva Santos", "email":"eva@ifrn.edu.br", "matricula":"999", "tipo":"SOLICITANTE"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Eva Santos"));
    }

    @Test
    void cancelamentoEPreservacaoDeFinalizados() throws Exception {
        Long id = abrir(1L, 2L, null);
        mvc.perform(patch("/chamados/{id}/cancelar", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitanteId\":2}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/chamados/{id}/cancelar", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitanteId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(patch("/chamados/{id}/assumir", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":3}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/chamados/{id}/cancelar", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"solicitanteId\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void solucaoEmBrancoViolaRegraDeNegocio() throws Exception {
        Long id = abrir(1L, 2L, null);
        servico.assumir(id, new AssumirEntrada(3L));
        mvc.perform(patch("/chamados/{id}/resolver", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":3,\"solucao\":\"   \"}"))
                .andExpect(status().isConflict());
        assertThat(chamados.findById(id).orElseThrow().getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
    }
}
