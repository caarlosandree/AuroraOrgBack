package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta de uma política de SLA.
 */
public record SlaPoliticaResponse(
    UUID id,
    String nome,
    TicketPriority prioridade,
    String prioridadeLabel,
    Integer prazoPrimeiraRespostaMinutos,
    Integer prazoResolucaoMinutos,
    Boolean ativa,
    UUID filaId,
    String filaNome,
    UUID categoriaId,
    String categoriaNome,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
