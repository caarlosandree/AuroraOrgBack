package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class FilaNotFoundException extends RuntimeException {

    public FilaNotFoundException(UUID filaId) {
        super(String.format("Fila não encontrada com ID: %s", filaId));
    }
}
