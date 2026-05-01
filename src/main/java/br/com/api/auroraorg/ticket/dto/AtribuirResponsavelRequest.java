package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request para atribuir um responsável a um chamado")
public record AtribuirResponsavelRequest(

    @NotNull(message = "O ID do responsável é obrigatório")
    @Schema(description = "ID do usuário responsável", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID responsavelId
) {
}
