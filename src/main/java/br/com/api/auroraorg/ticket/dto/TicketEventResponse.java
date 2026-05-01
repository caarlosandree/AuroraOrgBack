package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de resposta para eventos de histórico.
 *
 * Representa ações automáticas ou manuais registradas no histórico do chamado.
 */
public record TicketEventResponse(
    UUID id,
    UUID ticketId,
    TicketEventType eventType,
    String eventTypeLabel,
    TicketEventOrigin origin,
    ActorInfo actor,
    String title,
    String description,
    String oldValue,
    String newValue,
    Map<String, Object> metadata,
    CommentVisibility visibility,
    LocalDateTime createdAt
) {

    /**
     * Informações resumidas do ator para exibição.
     * Pode ser nulo para eventos do sistema.
     */
    public record ActorInfo(
        UUID id,
        String name,
        String email
    ) {
    }
}
