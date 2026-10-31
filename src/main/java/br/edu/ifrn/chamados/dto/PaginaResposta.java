package br.edu.ifrn.chamados.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResposta<T>(List<T> itens, int numeroPagina, int tamanhoPagina,
                                long totalElementos, int totalPaginas) {
    public static <T> PaginaResposta<T> de(Page<T> pagina) {
        return new PaginaResposta<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
