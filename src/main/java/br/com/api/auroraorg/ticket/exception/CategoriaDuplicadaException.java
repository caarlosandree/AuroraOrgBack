package br.com.api.auroraorg.ticket.exception;

public class CategoriaDuplicadaException extends RuntimeException {

    public CategoriaDuplicadaException(String name) {
        super(String.format("Já existe uma categoria com o nome '%s'", name));
    }
}
