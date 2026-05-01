package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um comentário não é encontrado.
 */
public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(UUID commentId) {
        super(String.format("Comentário não encontrado com ID: %s", commentId));
    }

    public CommentNotFoundException(UUID commentId, UUID ticketId) {
        super(String.format("Comentário %s não encontrado no chamado %s", commentId, ticketId));
    }
}
