package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

/**
 * Exceção lançada quando há tentativa de operação em comentário removido.
 */
public class CommentRemovedException extends RuntimeException {

    public CommentRemovedException(UUID commentId) {
        super(String.format("O comentário %s foi removido e não pode ser alterado", commentId));
    }
}
