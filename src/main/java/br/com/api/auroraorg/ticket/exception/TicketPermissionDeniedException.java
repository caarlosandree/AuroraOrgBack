package br.com.api.auroraorg.ticket.exception;

/**
 * Exceção lançada quando o usuário não tem permissão para realizar
 * uma operação em um chamado.
 */
public class TicketPermissionDeniedException extends RuntimeException {
    
    public TicketPermissionDeniedException(String action) {
        super(String.format("Permissão negada: você não pode %s", action));
    }
    
    public TicketPermissionDeniedException(String action, String reason) {
        super(String.format("Permissão negada para %s: %s", action, reason));
    }
}
