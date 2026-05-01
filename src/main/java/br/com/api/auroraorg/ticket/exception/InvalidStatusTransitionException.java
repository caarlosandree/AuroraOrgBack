package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

/**
 * Exceção lançada quando uma transição de status é inválida.
 */
public class InvalidStatusTransitionException extends RuntimeException {
    
    public InvalidStatusTransitionException(TicketStatus from, TicketStatus to) {
        super(String.format(
            "Transição de status inválida: não é possível mudar de '%s' para '%s'",
            from.getLabel(),
            to.getLabel()
        ));
    }
    
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
