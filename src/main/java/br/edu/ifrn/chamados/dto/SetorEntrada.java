package br.edu.ifrn.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetorEntrada(@NotBlank @Size(max = 120) String nome, @NotBlank @Size(max = 20) String sigla) {
}
