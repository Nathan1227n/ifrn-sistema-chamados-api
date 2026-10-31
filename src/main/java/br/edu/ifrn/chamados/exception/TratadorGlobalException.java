package br.edu.ifrn.chamados.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorGlobalException {
    private static final Logger LOG = LoggerFactory.getLogger(TratadorGlobalException.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, erro.getMessage(), requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> conflito(RegraNegocioException erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.CONFLICT, erro.getMessage(), requisicao);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<ErroResposta> integridade(Exception erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.CONFLICT, "Operação conflita com registros vinculados, dados únicos ou outra alteração simultânea", requisicao);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroResposta> validacao(BindException erro, HttpServletRequest requisicao) {
        String mensagem = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": valor inválido").distinct().sorted()
                .collect(java.util.stream.Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem.isBlank() ? "Dados inválidos" : mensagem, requisicao);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ErroResposta> entradaInvalida(Exception erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados, JSON ou parâmetros inválidos", requisicao);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(IllegalArgumentException erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", requisicao);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErroResposta> rotaInexistente(Exception erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodoInvalido(Exception erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido", requisicao);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> formatoInvalido(Exception erro, HttpServletRequest requisicao) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado", requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception erro, HttpServletRequest requisicao) {
        LOG.error("Falha inesperada ao processar {}", requisicao.getRequestURI(), erro);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", requisicao);
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, HttpServletRequest requisicao) {
        return ResponseEntity.status(status).body(new ErroResposta(LocalDateTime.now(), status.value(),
                mensagem, requisicao.getRequestURI()));
    }
}
