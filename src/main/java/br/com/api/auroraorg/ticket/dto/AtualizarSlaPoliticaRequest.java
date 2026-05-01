package br.com.api.auroraorg.ticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para atualização de uma política de SLA.
 */
public record AtualizarSlaPoliticaRequest(
    @NotBlank(message = "O nome da política é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String nome,

    @Min(value = 1, message = "O prazo de primeira resposta deve ser maior que 0")
    Integer prazoPrimeiraRespostaMinutos,

    @Min(value = 1, message = "O prazo de resolução deve ser maior que 0")
    Integer prazoResolucaoMinutos,

    UUID filaId,

    UUID categoriaId
) {}
