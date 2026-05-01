package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resposta com dados de uma categoria de chamado")
public record CategoriaResponse(

    @Schema(description = "ID da categoria", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID id,

    @Schema(description = "Nome da categoria", example = "Erro no sistema")
    String name,

    @Schema(description = "Descrição da categoria")
    String description,

    @Schema(description = "Indica se a categoria está ativa", example = "true")
    Boolean active,

    @Schema(description = "Fila padrão associada")
    FilaResponse filaPadrao,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data de atualização")
    LocalDateTime updatedAt
) {
}
