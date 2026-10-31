package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Chamado;
import java.util.List;

public record ChamadoDetalhe(ChamadoResumo chamado, List<InteracaoResposta> interacoes) {
    public static ChamadoDetalhe de(Chamado chamado) {
        return new ChamadoDetalhe(ChamadoResumo.de(chamado),
                chamado.getInteracoes().stream().map(InteracaoResposta::de).toList());
    }
}
