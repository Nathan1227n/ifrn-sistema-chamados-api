package br.edu.ifrn.chamados.exception;

import java.time.LocalDateTime;

public record ErroResposta(LocalDateTime timestamp, int status, String mensagem, String caminho) {
}
