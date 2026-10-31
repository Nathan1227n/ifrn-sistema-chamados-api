package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.config.ConfiguracaoWeb;
import br.edu.ifrn.chamados.dto.*;
import br.edu.ifrn.chamados.exception.*;
import br.edu.ifrn.chamados.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChamadoController.class)
@Import({TratadorGlobalException.class, ConfiguracaoWeb.class})
class ChamadoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ChamadoService servico;

    @Test
    void aberturaInvalidaRetorna400NoPadrao() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.caminho").value("/chamados")).andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(servico);
    }

    @Test
    void enumInvalidoRetorna400() throws Exception {
        mvc.perform(get("/chamados?status=INVALIDO")).andExpect(status().isBadRequest());
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identificadorInvalidoRetorna400() throws Exception {
        mvc.perform(get("/chamados/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/chamados/0")).andExpect(status().isBadRequest());
    }

    @Test
    void paginacaoInvalidaRetorna400() throws Exception {
        for (String consulta : new String[]{"page=-1", "size=0", "page=abc", "size=-5"}) {
            mvc.perform(get("/chamados?" + consulta)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void inexistenteRetorna404() throws Exception {
        when(servico.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado não encontrado"));
        mvc.perform(get("/chamados/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Chamado não encontrado"));
    }

    @Test
    void regraNegocioRetorna409() throws Exception {
        when(servico.assumir(eq(1L), any())).thenThrow(new RegraNegocioException("Técnico não pertence ao setor responsável"));
        mvc.perform(patch("/chamados/1/assumir").contentType(MediaType.APPLICATION_JSON).content("{\"tecnicoId\":4}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.caminho").value("/chamados/1/assumir"));
    }

    @Test
    void falhaInesperadaNaoExpoeDetalhesInternos() throws Exception {
        when(servico.buscar(1L)).thenThrow(new RuntimeException("informação interna sensível"));
        mvc.perform(get("/chamados/1")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value("Erro interno do servidor"));
    }
}
