package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class SlaNaoEncontradoException extends RuntimeException {

    public SlaNaoEncontradoException(UUID chamadoId) {
        super("SLA não encontrado para o chamado: " + chamadoId);
    }
}
