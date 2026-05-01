package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Status possíveis do SLA de um chamado.
 *
 * Regras:
 * - DENTRO_DO_PRAZO: chamado ainda não violou o SLA
 * - EM_RISCO: chamado próximo do vencimento (<= 20% do tempo restante)
 * - VENCIDO: chamado ultrapassou o prazo sem cumprir o objetivo
 * - CUMPRIDO: objetivo foi atingido dentro do prazo
 * - VIOLADO: objetivo foi atingido fora do prazo
 * - CANCELADO: SLA encerrado sem resolução (chamado cancelado)
 * - PAUSADO: SLA temporariamente suspenso (aguardando solicitante)
 */
@Getter
public enum SlaStatus {

    DENTRO_DO_PRAZO("Dentro do Prazo", "SLA ainda não foi violado", false, false),
    EM_RISCO("Em Risco", "SLA próximo do vencimento", false, false),
    VENCIDO("Vencido", "SLA foi violado", true, false),
    CUMPRIDO("Cumprido", "Objetivo atingido dentro do prazo", true, true),
    VIOLADO("Violado", "Objetivo atingido fora do prazo", true, false),
    CANCELADO("Cancelado", "SLA encerrado sem resolução", true, false),
    PAUSADO("Pausado", "SLA temporariamente suspenso", false, false);

    private final String label;
    private final String description;
    private final boolean finalState;
    private final boolean successful;

    SlaStatus(String label, String description, boolean finalState, boolean successful) {
        this.label = label;
        this.description = description;
        this.finalState = finalState;
        this.successful = successful;
    }

    /**
     * Verifica se o status é um estado final (não muda mais automaticamente).
     */
    public boolean isFinal() {
        return this.finalState;
    }

    /**
     * Verifica se o SLA foi cumprido com sucesso.
     */
    public boolean isSuccessful() {
        return this.successful;
    }

    /**
     * Verifica se o status permite monitoramento automático.
     */
    public boolean allowsMonitoring() {
        return this == DENTRO_DO_PRAZO || this == EM_RISCO || this == PAUSADO;
    }

    /**
     * Verifica se o status indica que o prazo foi ultrapassado.
     */
    public boolean isExpired() {
        return this == VENCIDO || this == VIOLADO;
    }
}
