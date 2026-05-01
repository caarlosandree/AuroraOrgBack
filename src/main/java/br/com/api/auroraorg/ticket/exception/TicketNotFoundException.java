package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um chamado não é encontrado.
 */
public class TicketNotFoundException extends RuntimeException {
    
    public TicketNotFoundException(UUID ticketId) {
        super(String.format("Chamado não encontrado com ID: %s", ticketId));
    }
    
    public TicketNotFoundException(String message) {
        super(message);
    }
}
