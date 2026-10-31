package br.edu.ifrn.chamados.specification;

import br.edu.ifrn.chamados.dto.ChamadoFiltro;
import br.edu.ifrn.chamados.model.Chamado;
import org.springframework.data.jpa.domain.Specification;

public final class ChamadoSpecification {
    private ChamadoSpecification() {
    }

    public static Specification<Chamado> filtrar(ChamadoFiltro filtro) {
        if (filtro.dataInicio() != null && filtro.dataFim() != null
                && filtro.dataInicio().isAfter(filtro.dataFim())) {
            throw new IllegalArgumentException("dataInicio não pode ser posterior a dataFim");
        }
        return Filtros.<Chamado>igual("status", filtro.status())
                .and(Filtros.igual("prioridade", filtro.prioridade()))
                .and(Filtros.igual("categoria.setor.id", filtro.setorId()))
                .and(Filtros.igual("categoria.id", filtro.categoriaId()))
                .and(Filtros.igual("solicitante.id", filtro.solicitanteId()))
                .and(Filtros.igual("tecnico.id", filtro.tecnicoId()))
                .and((raiz, consulta, cb) -> filtro.dataInicio() == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(raiz.get("dataAbertura"), filtro.dataInicio().atStartOfDay()))
                .and((raiz, consulta, cb) -> filtro.dataFim() == null ? cb.conjunction()
                        : cb.lessThan(raiz.get("dataAbertura"), filtro.dataFim().plusDays(1).atStartOfDay()));
    }
}
