package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResolverEntrada(@NotNull @Positive Long tecnicoId, @Size(max = 5000) String solucao) {
}
