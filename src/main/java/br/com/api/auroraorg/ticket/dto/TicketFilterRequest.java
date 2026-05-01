package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;

import java.util.UUID;

/**
 * DTO para filtros de listagem de chamados.
 * 
 * Todos os campos são opcionais. Quando informados,
 * combinam com AND lógico.
 */
public record TicketFilterRequest(
    TicketStatus status,
    TicketPriority priority,
    String category,
    UUID requesterId,
    UUID assigneeId,
    Boolean slaOverdue,
    String searchTerm  // Busca em título e descrição
) {}
