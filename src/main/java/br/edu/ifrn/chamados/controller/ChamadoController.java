package br.edu.ifrn.chamados.controller;

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
import br.edu.ifrn.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {
    private final ChamadoService chamados;

    @PostMapping
    public ResponseEntity<ChamadoResumo> abrir(@RequestBody @Valid ChamadoEntrada entrada) {
        ChamadoResumo resposta = chamados.abrir(entrada);
        return ResponseEntity.created(URI.create("/chamados/" + resposta.id())).body(resposta);
    }

    @GetMapping
    public PaginaResposta<ChamadoResumo> listar(@Valid @ModelAttribute ChamadoFiltro filtro, Pageable pagina) {
        return chamados.listar(filtro, pagina);
    }

    @GetMapping("/{id}")
    public ChamadoDetalhe buscar(@PathVariable @Positive Long id) {
        return chamados.buscar(id);
    }

    @PatchMapping("/{id}/assumir")
    public ChamadoResumo assumir(@PathVariable @Positive Long id, @RequestBody @Valid AssumirEntrada entrada) {
        return chamados.assumir(id, entrada);
    }

    @PatchMapping("/{id}/resolver")
    public ChamadoResumo resolver(@PathVariable @Positive Long id, @RequestBody @Valid ResolverEntrada entrada) {
        return chamados.resolver(id, entrada);
    }

    @PatchMapping("/{id}/cancelar")
    public ChamadoResumo cancelar(@PathVariable @Positive Long id, @RequestBody @Valid CancelarEntrada entrada) {
        return chamados.cancelar(id, entrada);
    }

    @PostMapping("/{id}/interacoes")
    public ResponseEntity<InteracaoResposta> interagir(@PathVariable @Positive Long id,
            @RequestBody @Valid InteracaoEntrada entrada) {
        InteracaoResposta resposta = chamados.interagir(id, entrada);
        return ResponseEntity.created(URI.create("/chamados/" + id)).body(resposta);
    }
}
