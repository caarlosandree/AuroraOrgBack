package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO unificado para itens da timeline de chamados.
 *
 * Representa tanto comentários quanto eventos de histórico em um formato consistente
 * para exibição na interface.
 *
 * Tipos de item:
 * - COMENTARIO: comentário de usuário
 * - EVENTO: evento de histórico automático ou manual
 */
public record TimelineItemResponse(
    UUID id,
    TimelineItemType type,
    String title,
    String description,
    AuthorInfo author,
    CommentVisibility visibility,
    LocalDateTime createdAt,

    // Campos específicos para comentários
    String content,
    Boolean edited,
    Boolean removed,

    // Campos específicos para eventos
    TicketEventType eventType,
    String eventTypeLabel,
    TicketEventOrigin origin,
    String oldValue,
    String newValue,
    Map<String, Object> metadata
) {

    /**
     * Tipo de item na timeline.
     */
    public enum TimelineItemType {
        COMENTARIO,
        EVENTO
    }

    /**
     * Informações do autor/ator.
     */
    public record AuthorInfo(
        UUID id,
        String name,
        String email
    ) {
    }

    /**
     * Factory method para criar um item de comentário.
     */
    public static TimelineItemResponse fromComment(CommentResponse comment) {
        return new TimelineItemResponse(
            comment.id(),
            TimelineItemType.COMENTARIO,
            comment.author() != null ? comment.author().name() + " adicionou um comentário" : "Comentário adicionado",
            null,
            comment.author() != null ? new AuthorInfo(comment.author().id(), comment.author().name(), comment.author().email()) : null,
            comment.visibility(),
            comment.createdAt(),
            comment.content(),
            comment.edited(),
            comment.removed(),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Factory method para criar um item de evento.
     */
    public static TimelineItemResponse fromEvent(TicketEventResponse event) {
        return new TimelineItemResponse(
            event.id(),
            TimelineItemType.EVENTO,
            event.title(),
            event.description(),
            event.actor() != null ? new AuthorInfo(event.actor().id(), event.actor().name(), event.actor().email()) : null,
            event.visibility(),
            event.createdAt(),
            null,
            null,
            null,
            event.eventType(),
            event.eventTypeLabel(),
            event.origin(),
            event.oldValue(),
            event.newValue(),
            event.metadata()
        );
    }

    /**
     * Verifica se é um comentário.
     */
    public boolean isComment() {
        return type == TimelineItemType.COMENTARIO;
    }

    /**
     * Verifica se é um evento.
     */
    public boolean isEvent() {
        return type == TimelineItemType.EVENTO;
    }

    /**
     * Verifica se é visível publicamente.
     */
    public boolean isPublic() {
        return visibility == CommentVisibility.PUBLICO;
    }
}
