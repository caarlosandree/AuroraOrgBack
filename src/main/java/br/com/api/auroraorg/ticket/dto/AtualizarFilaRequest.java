package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para atualização de uma fila de atendimento")
public record AtualizarFilaRequest(

    @NotBlank(message = "O nome da fila é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre {min} e {max} caracteres")
    @Schema(description = "Nome da fila", example = "Suporte Técnico")
    String name,

    @Size(max = 500, message = "A descrição deve ter no máximo {max} caracteres")
    @Schema(description = "Descrição da fila", example = "Atendimento de problemas técnicos e infraestrutura")
    String description
) {
}
