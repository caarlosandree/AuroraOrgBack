package br.com.api.auroraorg.ticket.exception;

public class AgenteJaVinculadoException extends RuntimeException {

    public AgenteJaVinculadoException() {
        super("O agente já está vinculado a esta fila");
    }
}
