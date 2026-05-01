package br.com.api.auroraorg.ticket.exception;

public class AnexoPermissaoNegadaException extends RuntimeException {

    public AnexoPermissaoNegadaException(String operacao) {
        super("Permissão negada para " + operacao + " neste anexo.");
    }

    public AnexoPermissaoNegadaException(String operacao, String detalhe) {
        super("Permissão negada para " + operacao + ". " + detalhe);
    }
}
