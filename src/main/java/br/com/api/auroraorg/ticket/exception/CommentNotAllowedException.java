package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

/**
 * Exceção lançada quando não é permitido adicionar/editar comentário.
 */
public class CommentNotAllowedException extends RuntimeException {

    public CommentNotAllowedException(String message) {
        super(message);
    }

    public CommentNotAllowedException(TicketStatus status) {
        super(String.format("Não é permitido comentar em chamados com status %s", status.getLabel()));
    }

    public CommentNotAllowedException(String visibility, String role) {
        super(String.format("Usuários com perfil %s não podem criar comentários %s", role, visibility));
    }
}
