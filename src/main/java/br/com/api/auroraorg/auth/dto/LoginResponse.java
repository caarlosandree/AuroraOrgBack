package br.com.api.auroraorg.auth.dto;

import br.com.api.auroraorg.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de login com token JWT")
public record LoginResponse(
        @Schema(description = "Token de acesso JWT", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tokenType,

        @Schema(description = "Tempo de expiração em segundos", example = "7200")
        Long expiresIn,

        @Schema(description = "Dados do usuário autenticado")
        UserResponse user
) {
}
