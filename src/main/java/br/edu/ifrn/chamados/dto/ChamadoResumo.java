package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.enums.Prioridade;
import br.edu.ifrn.chamados.enums.StatusChamado;
import br.edu.ifrn.chamados.model.Chamado;
import java.time.LocalDateTime;

public record ChamadoResumo(
        Long id,
        String titulo,
        String descricao,
        String local,
        Prioridade prioridade,
        StatusChamado status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        String solucao,
        UsuarioResposta solicitante,
        CategoriaResposta categoria,
        UsuarioResposta tecnico) {
    public static ChamadoResumo de(Chamado entidade) {
        return new ChamadoResumo(
                entidade.getId(),
                entidade.getTitulo(),
                entidade.getDescricao(),
                entidade.getLocal(),
                entidade.getPrioridade(),
                entidade.getStatus(),
                entidade.getDataAbertura(),
                entidade.getDataFechamento(),
                entidade.getSolucao(),
                UsuarioResposta.de(entidade.getSolicitante()),
                CategoriaResposta.de(entidade.getCategoria()),
                entidade.getTecnico() == null ? null : UsuarioResposta.de(entidade.getTecnico()));
    }
}
