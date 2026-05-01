package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resumo de chamado para listagens.
 * 
 * Versão enxuta com apenas os campos essenciais para exibição
 * em listas, tabelas e dashboards.
 */
public record TicketSummaryResponse(
    UUID id,
    String title,
    TicketStatus status,
    String statusLabel,
    TicketPriority priority,
    String priorityLabel,
    String category,
    String requesterName,
    String assigneeName,
    LocalDateTime createdAt,
    LocalDateTime slaDueAt,
    Boolean slaOverdue
) {}
