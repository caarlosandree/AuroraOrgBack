package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request para definir a fila padrão de uma categoria")
public record DefinirFilaPadraoCategoriaRequest(

    @NotNull(message = "O ID da fila padrão é obrigatório")
    @Schema(description = "ID da fila padrão", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID filaPadraoId
) {
}
