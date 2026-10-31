package br.edu.ifrn.chamados.service;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Paginacao {
    private Paginacao() {
    }

    public static Pageable validar(Pageable pagina, String... camposPermitidos) {
        Set<String> campos = Set.of(camposPermitidos);
        for (Sort.Order ordem : pagina.getSort()) {
            if (!campos.contains(ordem.getProperty())) {
                throw new IllegalArgumentException("Campo de ordenação inválido: " + ordem.getProperty());
            }
        }
        Sort ordenacao = pagina.getSort();
        if (ordenacao.getOrderFor("id") == null) {
            ordenacao = ordenacao.and(Sort.by("id"));
        }
        return PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), 50), ordenacao);
    }
}
