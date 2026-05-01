package br.com.api.auroraorg.ticket.exception;

import br.com.api.auroraorg.ticket.enums.TicketStatus;

public class ChamadoNaoPermiteAtualizacaoSlaException extends RuntimeException {

    public ChamadoNaoPermiteAtualizacaoSlaException(TicketStatus status) {
        super("Chamado com status '" + status.getLabel() + "' não permite atualização de SLA.");
    }
}
