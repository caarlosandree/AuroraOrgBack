package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Resposta detalhada de uma fila, incluindo agentes vinculados")
public record FilaDetalheResponse(

    @Schema(description = "ID da fila", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID id,

    @Schema(description = "Nome da fila", example = "Suporte Técnico")
    String name,

    @Schema(description = "Descrição da fila")
    String description,

    @Schema(description = "Indica se a fila está ativa", example = "true")
    Boolean active,

    @Schema(description = "Lista de agentes vinculados à fila")
    List<FilaAgenteResponse> agentes,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data de atualização")
    LocalDateTime updatedAt
) {
}
