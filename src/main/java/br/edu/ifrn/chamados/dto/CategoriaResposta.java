package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Categoria;

public record CategoriaResposta(Long id, String nome, String descricao, SetorResposta setor) {
    public static CategoriaResposta de(Categoria entidade) {
        return new CategoriaResposta(
                entidade.getId(),
                entidade.getNome(),
                entidade.getDescricao(),
                SetorResposta.de(entidade.getSetor()));
    }
}
