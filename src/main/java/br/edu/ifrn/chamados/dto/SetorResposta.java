package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Setor;

public record SetorResposta(Long id, String nome, String sigla) {
    public static SetorResposta de(Setor entidade) {
        return new SetorResposta(entidade.getId(), entidade.getNome(), entidade.getSigla());
    }
}
