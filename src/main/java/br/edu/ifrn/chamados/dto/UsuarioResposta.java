package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.enums.TipoUsuario;
import br.edu.ifrn.chamados.model.Usuario;

public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        String matricula,
        TipoUsuario tipo,
        SetorResposta setor) {
    public static UsuarioResposta de(Usuario entidade) {
        return new UsuarioResposta(
                entidade.getId(),
                entidade.getNome(),
                entidade.getEmail(),
                entidade.getMatricula(),
                entidade.getTipo(),
                entidade.getSetor() == null ? null : SetorResposta.de(entidade.getSetor()));
    }
}
