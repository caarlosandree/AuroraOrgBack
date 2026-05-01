package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class SlaPoliticaNaoEncontradaException extends RuntimeException {

    public SlaPoliticaNaoEncontradaException(UUID id) {
        super("Política de SLA não encontrada: " + id);
    }

    public SlaPoliticaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
