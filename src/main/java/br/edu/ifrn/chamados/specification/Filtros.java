package br.edu.ifrn.chamados.specification;

import jakarta.persistence.criteria.Path;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class Filtros {
    private Filtros() {
    }

    public static <T> Specification<T> igual(String campo, Object valor) {
        return (raiz, consulta, cb) -> valor == null ? cb.conjunction() : cb.equal(caminho(raiz, campo), valor);
    }

    public static <T> Specification<T> nomeContem(String nome) {
        return (raiz, consulta, cb) -> nome == null || nome.isBlank() ? cb.conjunction()
                : cb.like(cb.lower(raiz.get("nome")), "%" + escapar(nome.trim().toLowerCase(Locale.ROOT)) + "%", '\\');
    }

    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Path<?> caminho(Path<?> raiz, String campo) {
        Path<?> caminho = raiz;
        for (String parte : campo.split("\\.")) {
            caminho = caminho.get(parte);
        }
        return caminho;
    }
}
