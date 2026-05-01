package br.com.api.auroraorg.ticket.exception;

public class ResponsavelNaoPertenceFilaException extends RuntimeException {

    public ResponsavelNaoPertenceFilaException() {
        super("O responsável atual não pertence à nova fila. Remova o responsável antes de transferir ou atribua um novo responsável da fila destino.");
    }
}
