package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.CategoriaEntrada;
import br.edu.ifrn.chamados.dto.CategoriaResposta;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.service.CategoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaService categorias;

    @PostMapping
    public ResponseEntity<CategoriaResposta> criar(@RequestBody @Valid CategoriaEntrada entrada) {
        CategoriaResposta resposta = categorias.criar(entrada);
        return ResponseEntity.created(URI.create("/categorias/" + resposta.id())).body(resposta);
    }

    @GetMapping
    public PaginaResposta<CategoriaResposta> listar(@RequestParam(required = false) String nome, @RequestParam(required = false) @Positive Long setorId, Pageable pagina) {
        return categorias.listar(nome, setorId, pagina);
    }

    @GetMapping("/{id}")
    public CategoriaResposta buscar(@PathVariable @Positive Long id) {
        return categorias.buscar(id);
    }

    @PutMapping("/{id}")
    public CategoriaResposta atualizar(@PathVariable @Positive Long id, @RequestBody @Valid CategoriaEntrada entrada) {
        return categorias.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable @Positive Long id) {
        categorias.excluir(id);
    }
}
