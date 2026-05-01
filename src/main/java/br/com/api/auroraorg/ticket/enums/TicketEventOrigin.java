package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Origem do evento no histórico de chamados.
 *
 * Determina se o evento foi gerado por uma ação de usuário ou automaticamente pelo sistema.
 */
@Getter
public enum TicketEventOrigin {

    USUARIO("Usuário", "Ação realizada por um usuário do sistema"),
    SISTEMA("Sistema", "Evento gerado automaticamente pelo sistema");

    private final String label;
    private final String description;

    TicketEventOrigin(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * Verifica se a origem é o sistema.
     */
    public boolean isSystem() {
        return this == SISTEMA;
    }

    /**
     * Verifica se a origem é um usuário.
     */
    public boolean isUser() {
        return this == USUARIO;
    }
}
