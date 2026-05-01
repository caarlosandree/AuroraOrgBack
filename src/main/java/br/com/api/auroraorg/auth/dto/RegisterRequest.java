package br.com.api.auroraorg.auth.dto;

import br.com.api.auroraorg.user.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição de registro de novo usuário")
public record RegisterRequest(
        @Schema(description = "Nome do usuário", example = "João Silva")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Schema(description = "Email do usuário", example = "joao.silva@email.com")
        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O email deve ser válido")
        String email,

        @Schema(description = "Senha do usuário", example = "senha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @Schema(description = "Perfil do usuário", example = "SOLICITANTE")
        @NotNull(message = "O perfil é obrigatório")
        UserRole role
) {
}
