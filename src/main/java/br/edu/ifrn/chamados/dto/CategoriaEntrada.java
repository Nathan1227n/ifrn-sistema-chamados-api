package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CategoriaEntrada(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 1000) String descricao,
        @NotNull @Positive Long setorId) {
}
