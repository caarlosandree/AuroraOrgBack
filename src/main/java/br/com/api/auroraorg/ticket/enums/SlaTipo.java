package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Tipo de SLA aplicado a um chamado.
 *
 * - PRIMEIRA_RESPOSTA: tempo máximo para um agente interagir pela primeira vez
 * - RESOLUCAO: tempo máximo para resolver o chamado
 */
@Getter
public enum SlaTipo {

    PRIMEIRA_RESPOSTA("Primeira Resposta", "Tempo máximo para primeira interação de um agente"),
    RESOLUCAO("Resolução", "Tempo máximo para resolver o chamado");

    private final String label;
    private final String description;

    SlaTipo(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * Verifica se é SLA de primeira resposta.
     */
    public boolean isPrimeiraResposta() {
        return this == PRIMEIRA_RESPOSTA;
    }

    /**
     * Verifica se é SLA de resolução.
     */
    public boolean isResolucao() {
        return this == RESOLUCAO;
    }
}
