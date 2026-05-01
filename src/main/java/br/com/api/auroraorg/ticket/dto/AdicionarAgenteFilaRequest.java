package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request para adicionar um agente a uma fila")
public record AdicionarAgenteFilaRequest(

    @NotNull(message = "O ID do agente é obrigatório")
    @Schema(description = "ID do usuário agente", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID agenteId
) {
}
