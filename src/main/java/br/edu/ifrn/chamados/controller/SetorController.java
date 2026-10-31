package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.CategoriaResposta;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.dto.SetorEntrada;
import br.edu.ifrn.chamados.dto.SetorResposta;
import br.edu.ifrn.chamados.service.SetorService;
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
@RequestMapping("/setores")
@RequiredArgsConstructor
public class SetorController {
    private final SetorService setores;

    @PostMapping
    public ResponseEntity<SetorResposta> criar(@RequestBody @Valid SetorEntrada entrada) {
        SetorResposta resposta = setores.criar(entrada);
        return ResponseEntity.created(URI.create("/setores/" + resposta.id())).body(resposta);
    }

    @GetMapping
    public PaginaResposta<SetorResposta> listar(@RequestParam(required = false) String nome, Pageable pagina) {
        return setores.listar(nome, pagina);
    }

    @GetMapping("/{id}")
    public SetorResposta buscar(@PathVariable @Positive Long id) {
        return setores.buscar(id);
    }

    @PutMapping("/{id}")
    public SetorResposta atualizar(@PathVariable @Positive Long id, @RequestBody @Valid SetorEntrada entrada) {
        return setores.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable @Positive Long id) {
        setores.excluir(id);
    }

    @GetMapping("/{id}/categorias")
    public PaginaResposta<CategoriaResposta> listarCategorias(@PathVariable @Positive Long id, Pageable pagina) {
        return setores.listarCategorias(id, pagina);
    }
}
