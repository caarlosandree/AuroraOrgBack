package br.com.api.auroraorg.user.dto;

import br.com.api.auroraorg.user.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta com dados do usuário")
public record UserResponse(
        @Schema(description = "ID do usuário", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID id,

        @Schema(description = "Nome do usuário", example = "João Silva")
        String name,

        @Schema(description = "Email do usuário", example = "joao.silva@email.com")
        String email,

        @Schema(description = "Perfil do usuário", example = "SOLICITANTE")
        UserRole role,

        @Schema(description = "Status ativo do usuário", example = "true")
        Boolean active,

        @Schema(description = "Data de criação", example = "2026-04-30T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Data de atualização", example = "2026-04-30T10:00:00")
        LocalDateTime updatedAt
) {
}
