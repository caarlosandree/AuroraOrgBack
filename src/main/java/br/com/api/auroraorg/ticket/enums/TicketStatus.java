package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Status possíveis de um chamado/ticket no sistema de ITSM.
 * 
 * Fluxo esperado:
 * - ABERTO → EM_TRIAGEM | CANCELADO
 * - EM_TRIAGEM → EM_ATENDIMENTO | CANCELADO
 * - EM_ATENDIMENTO → AGUARDANDO_SOLICITANTE | RESOLVIDO | CANCELADO
 * - AGUARDANDO_SOLICITANTE → EM_ATENDIMENTO | CANCELADO
 * - RESOLVIDO → FECHADO | EM_ATENDIMENTO (reabertura)
 * - FECHADO → (terminal)
 * - CANCELADO → (terminal)
 */
@Getter
public enum TicketStatus {
    
    ABERTO("Aberto", "Chamado recém-criado, aguardando triagem"),
    EM_TRIAGEM("Em Triagem", "Sendo avaliado e classificado"),
    EM_ATENDIMENTO("Em Atendimento", "Em processo de resolução"),
    AGUARDANDO_SOLICITANTE("Aguardando Solicitante", "Pendente de informações do solicitante"),
    RESOLVIDO("Resolvido", "Solução aplicada, aguardando fechamento"),
    FECHADO("Fechado", "Chamado encerrado definitivamente"),
    CANCELADO("Cancelado", "Chamado cancelado pelo solicitante ou sistema");
    
    private final String label;
    private final String description;
    
    TicketStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }
    
    /**
     * Verifica se o status é terminal (não permite mais alterações).
     */
    public boolean isTerminal() {
        return this == FECHADO || this == CANCELADO;
    }
    
    /**
     * Verifica se o status permite que o solicitante cancele.
     */
    public boolean canBeCancelledByRequester() {
        return this == ABERTO || 
               this == EM_TRIAGEM || 
               this == EM_ATENDIMENTO || 
               this == AGUARDANDO_SOLICITANTE;
    }
    
    /**
     * Verifica se o status permite alteração de responsável.
     */
    public boolean allowsAssigneeChange() {
        return !isTerminal();
    }
    
    /**
     * Verifica se o status permite alteração pelos atendentes/admin.
     */
    public boolean allowsOperationalChanges() {
        return this != FECHADO && this != CANCELADO;
    }
}
