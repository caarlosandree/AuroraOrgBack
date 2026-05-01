package br.com.api.auroraorg.ticket.exception;

public class AnexoStorageException extends RuntimeException {

    public AnexoStorageException(String message) {
        super(message);
    }

    public AnexoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
