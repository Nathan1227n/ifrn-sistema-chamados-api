package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InteracaoEntrada(@NotNull @Positive Long autorId, @NotBlank @Size(max = 5000) String texto) {
}
