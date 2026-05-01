package br.com.api.auroraorg.shared.exception;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException() {
        super("Usuário inativo. Entre em contato com o administrador do sistema.");
    }
}
