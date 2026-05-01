package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

/**
 * Exceção lançada quando uma operação inválida é tentada em um chamado.
 * Ex: tentar alterar chamado fechado, cancelar chamado já resolvido, etc.
 */
public class InvalidTicketOperationException extends RuntimeException {
    
    public InvalidTicketOperationException(String message) {
        super(message);
    }
    
    public InvalidTicketOperationException(TicketStatus status, String operation) {
        super(String.format(
            "Operação inválida: não é possível %s um chamado com status '%s'",
            operation,
            status.getLabel()
        ));
    }
}
