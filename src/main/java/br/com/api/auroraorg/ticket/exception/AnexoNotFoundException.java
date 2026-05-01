package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class AnexoNotFoundException extends RuntimeException {

    public AnexoNotFoundException(UUID anexoId) {
        super("Anexo não encontrado: " + anexoId);
    }

    public AnexoNotFoundException(UUID anexoId, UUID ticketId) {
        super("Anexo " + anexoId + " não encontrado no chamado " + ticketId);
    }
}
