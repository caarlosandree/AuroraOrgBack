package br.com.api.auroraorg.ticket.exception;

/**
 * Exceção lançada quando há tentativa de operação não permitida em comentário.
 */
public class CommentPermissionDeniedException extends RuntimeException {

    public CommentPermissionDeniedException(String operation) {
        super(String.format("Permissão negada para %s", operation));
    }

    public CommentPermissionDeniedException(String operation, String reason) {
        super(String.format("Permissão negada para %s. %s", operation, reason));
    }
}
