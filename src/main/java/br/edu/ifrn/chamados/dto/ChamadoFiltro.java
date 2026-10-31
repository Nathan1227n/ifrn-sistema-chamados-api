package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.enums.Prioridade;
import br.edu.ifrn.chamados.enums.StatusChamado;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ChamadoFiltro(StatusChamado status, Prioridade prioridade,
        @Positive Long setorId, @Positive Long categoriaId, @Positive Long solicitanteId,
        @Positive Long tecnicoId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
}
