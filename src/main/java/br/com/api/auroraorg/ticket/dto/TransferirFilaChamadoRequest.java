package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request para transferir um chamado para outra fila")
public record TransferirFilaChamadoRequest(

    @NotNull(message = "O ID da nova fila é obrigatório")
    @Schema(description = "ID da fila destino", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    UUID filaId
) {
}
