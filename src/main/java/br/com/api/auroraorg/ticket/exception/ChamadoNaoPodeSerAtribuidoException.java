package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

public class ChamadoNaoPodeSerAtribuidoException extends RuntimeException {

    public ChamadoNaoPodeSerAtribuidoException(TicketStatus status) {
        super(String.format("Chamado com status '%s' não pode ser atribuído ou alterado", status.getLabel()));
    }
}
