package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resposta com dados de um agente vinculado a uma fila")
public record FilaAgenteResponse(

    @Schema(description = "Dados do agente")
    UserResponse agente,

    @Schema(description = "Data de vínculo do agente à fila")
    LocalDateTime createdAt
) {
}
