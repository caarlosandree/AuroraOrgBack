package br.com.api.auroraorg.ticket.exception;

public class LimiteAnexosExcedidoException extends RuntimeException {

    public LimiteAnexosExcedidoException(int limite) {
        super("Limite de anexos por chamado atingido. Máximo permitido: " + limite);
    }
}
