package br.com.api.auroraorg.ticket.exception;

public class FilaDuplicadaException extends RuntimeException {

    public FilaDuplicadaException(String name) {
        super(String.format("Já existe uma fila com o nome '%s'", name));
    }
}
