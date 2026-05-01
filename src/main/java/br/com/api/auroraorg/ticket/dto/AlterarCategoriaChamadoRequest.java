package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request para alterar a categoria de um chamado")
public record AlterarCategoriaChamadoRequest(

    @NotNull(message = "O ID da nova categoria é obrigatório")
    @Schema(description = "ID da nova categoria", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID categoriaId
) {
}
