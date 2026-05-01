package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para alteração de status de um chamado.
 * 
 * A transição é validada contra a matriz de transições permitidas.
 * Apenas ADMIN, AGENTE ou GESTOR podem alterar status operacionais.
 */
public record UpdateStatusRequest(
    
    @NotNull(message = "O status é obrigatório")
    TicketStatus status
) {}
