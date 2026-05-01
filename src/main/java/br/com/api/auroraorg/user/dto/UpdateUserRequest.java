package br.com.api.auroraorg.user.dto;

import br.com.api.auroraorg.user.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para atualizar usuário")
public record UpdateUserRequest(
        @Schema(description = "Nome do usuário", example = "João Silva Atualizado")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Schema(description = "Perfil do usuário", example = "AGENTE")
        @NotNull(message = "O perfil é obrigatório")
        UserRole role
) {
}
