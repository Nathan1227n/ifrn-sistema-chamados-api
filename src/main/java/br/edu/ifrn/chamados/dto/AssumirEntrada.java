package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssumirEntrada(@NotNull @Positive Long tecnicoId) {
}
