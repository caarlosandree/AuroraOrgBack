package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

/**
 * Exceção lançada quando um evento de histórico não é encontrado.
 */
public class TicketEventNotFoundException extends RuntimeException {

    public TicketEventNotFoundException(UUID eventId) {
        super(String.format("Evento de histórico não encontrado com ID: %s", eventId));
    }
}
