package br.com.api.auroraorg.shared.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super(String.format("Já existe um usuário cadastrado com o email: '%s'", email));
    }
}
