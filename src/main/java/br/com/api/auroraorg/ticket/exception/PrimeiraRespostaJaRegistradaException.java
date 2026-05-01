package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class PrimeiraRespostaJaRegistradaException extends RuntimeException {

    public PrimeiraRespostaJaRegistradaException(UUID chamadoId) {
        super("Primeira resposta já registrada para o chamado: " + chamadoId);
    }
}
