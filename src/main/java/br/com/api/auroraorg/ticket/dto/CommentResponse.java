package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para comentários.
 *
 * Inclui informações do autor formatadas e flags de estado.
 */
public record CommentResponse(
    UUID id,
    UUID ticketId,
    AuthorInfo author,
    String content,
    CommentVisibility visibility,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean edited,
    boolean removed
) {

    /**
     * Informações resumidas do autor para exibição na timeline.
     */
    public record AuthorInfo(
        UUID id,
        String name,
        String email
    ) {
    }

    /**
     * Factory method para criar um comentário removido.
     * Usado quando o comentário foi removido mas precisa aparecer na lista.
     */
    public static CommentResponse createRemovedResponse(UUID id, UUID ticketId, LocalDateTime createdAt) {
        return new CommentResponse(
            id,
            ticketId,
            null,
            "[Comentário removido]",
            CommentVisibility.INTERNO,
            createdAt,
            createdAt,
            false,
            true
        );
    }
}
