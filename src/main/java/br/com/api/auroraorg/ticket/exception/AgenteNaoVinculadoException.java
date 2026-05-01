package br.com.api.auroraorg.ticket.exception;

public class AgenteNaoVinculadoException extends RuntimeException {

    public AgenteNaoVinculadoException() {
        super("O agente não está vinculado a esta fila");
    }
}
