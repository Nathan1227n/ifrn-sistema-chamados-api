package br.edu.ifrn.chamados.controller;

import br.edu.ifrn.chamados.dto.ChamadoResumo;
import br.edu.ifrn.chamados.dto.PaginaResposta;
import br.edu.ifrn.chamados.dto.UsuarioEntrada;
import br.edu.ifrn.chamados.dto.UsuarioResposta;
import br.edu.ifrn.chamados.enums.StatusChamado;
import br.edu.ifrn.chamados.enums.TipoUsuario;
import br.edu.ifrn.chamados.service.ChamadoService;
import br.edu.ifrn.chamados.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarios;
    private final ChamadoService chamados;

    @PostMapping
    public ResponseEntity<UsuarioResposta> criar(@RequestBody @Valid UsuarioEntrada entrada) {
        UsuarioResposta resposta = usuarios.criar(entrada);
        return ResponseEntity.created(URI.create("/usuarios/" + resposta.id())).body(resposta);
    }

    @GetMapping
    public PaginaResposta<UsuarioResposta> listar(@RequestParam(required = false) String nome, @RequestParam(required = false) TipoUsuario tipo, @RequestParam(required = false) @Positive Long setorId, Pageable pagina) {
        return usuarios.listar(nome, tipo, setorId, pagina);
    }

    @GetMapping("/{id}")
    public UsuarioResposta buscar(@PathVariable @Positive Long id) {
        return usuarios.buscar(id);
    }

    @PutMapping("/{id}")
    public UsuarioResposta atualizar(@PathVariable @Positive Long id, @RequestBody @Valid UsuarioEntrada entrada) {
        return usuarios.atualizar(id, entrada);
    }

    @GetMapping("/{id}/chamados")
    public PaginaResposta<ChamadoResumo> listarChamados(@PathVariable @Positive Long id,
            @RequestParam(required = false) StatusChamado status, Pageable pagina) {
        return chamados.listarPorUsuario(id, status, pagina);
    }
}
