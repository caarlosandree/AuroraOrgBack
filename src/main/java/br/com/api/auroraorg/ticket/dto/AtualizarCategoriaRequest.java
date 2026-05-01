package br.com.api.auroraorg.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para atualização de uma categoria de chamado")
public record AtualizarCategoriaRequest(

    @NotBlank(message = "O nome da categoria é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre {min} e {max} caracteres")
    @Schema(description = "Nome da categoria", example = "Erro no sistema")
    String name,

    @Size(max = 500, message = "A descrição deve ter no máximo {max} caracteres")
    @Schema(description = "Descrição da categoria", example = "Problemas e falhas em sistemas internos")
    String description
) {
}
