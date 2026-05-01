package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta detalhada de um chamado.
 * 
 * Inclui todas as informações relevantes para exibição completa,
 * incluindo dados do solicitante e responsável.
 */
public record TicketResponse(
    UUID id,
    String title,
    String description,
    TicketStatus status,
    String statusLabel,
    TicketPriority priority,
    String priorityLabel,
    String category,
    UserResponse requester,
    UserResponse assignee,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime slaDueAt,
    LocalDateTime resolvedAt,
    Long remainingSlaHours,
    Boolean slaOverdue
) {}
