package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class SlaNaoPodeSerRecalculadoException extends RuntimeException {

    public SlaNaoPodeSerRecalculadoException(UUID chamadoId) {
        super("SLA do chamado " + chamadoId + " não pode ser recalculado. Chamado já foi resolvido, fechado ou cancelado.");
    }
}
