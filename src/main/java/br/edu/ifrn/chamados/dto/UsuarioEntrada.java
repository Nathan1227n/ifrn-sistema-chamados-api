package br.edu.ifrn.chamados.dto;

import br.edu.ifrn.chamados.enums.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsuarioEntrada(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String matricula,
        @NotNull TipoUsuario tipo,
        @Positive Long setorId) {
}
