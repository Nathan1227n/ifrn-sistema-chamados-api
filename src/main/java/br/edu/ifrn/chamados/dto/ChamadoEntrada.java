package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.enums.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChamadoEntrada(
        @NotBlank @Size(max = 160) String titulo,
        @NotBlank @Size(max = 5000) String descricao,
        @NotBlank @Size(max = 200) String local,
        Prioridade prioridade,
        @NotNull @Positive Long categoriaId,
        @NotNull @Positive Long solicitanteId) {
}
