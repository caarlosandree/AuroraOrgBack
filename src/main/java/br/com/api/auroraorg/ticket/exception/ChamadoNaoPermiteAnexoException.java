package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

public class ChamadoNaoPermiteAnexoException extends RuntimeException {

    public ChamadoNaoPermiteAnexoException(TicketStatus status) {
        super("Chamado com status '" + status.getLabel() + "' não permite novos anexos.");
    }
}
