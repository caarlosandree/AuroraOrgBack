package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * DTO para criação de uma política de SLA.
 */
public record CriarSlaPoliticaRequest(
    @NotBlank(message = "O nome da política é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String nome,

    @NotNull(message = "A prioridade é obrigatória")
    TicketPriority prioridade,

    @NotNull(message = "O prazo de primeira resposta é obrigatório")
    @Min(value = 1, message = "O prazo de primeira resposta deve ser maior que 0")
    Integer prazoPrimeiraRespostaMinutos,

    @NotNull(message = "O prazo de resolução é obrigatório")
    @Min(value = 1, message = "O prazo de resolução deve ser maior que 0")
    Integer prazoResolucaoMinutos,

    UUID filaId,

    UUID categoriaId
) {}
