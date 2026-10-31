package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.model.Interacao;
import java.time.LocalDateTime;

public record InteracaoResposta(Long id, String texto, LocalDateTime dataHora, UsuarioResposta autor) {
    public static InteracaoResposta de(Interacao entidade) {
        return new InteracaoResposta(
                entidade.getId(),
                entidade.getTexto(),
                entidade.getDataHora(),
                UsuarioResposta.de(entidade.getAutor()));
    }
}
