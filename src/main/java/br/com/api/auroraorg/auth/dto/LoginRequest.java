package br.com.api.auroraorg.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição de login")
public record LoginRequest(
        @Schema(description = "Email do usuário", example = "usuario@email.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ser válido")
        String email,

        @Schema(description = "Senha do usuário", example = "senha123")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {
}
