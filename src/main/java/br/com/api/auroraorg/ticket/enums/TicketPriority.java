package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

import java.time.Duration;

/**
 * Prioridades de um chamado/ticket no sistema de ITSM.
 * 
 * Regras de SLA (Service Level Agreement):
 * - BAIXA: 72 horas
 * - MEDIA: 48 horas
 * - ALTA: 24 horas
 * - CRITICA: 4 horas
 * 
 * Futuras melhorias:
 * - Calendário comercial (dias úteis apenas)
 * - Horários de atendimento (ex: 8h às 18h)
 * - Feriados configuráveis
 * - SLA por categoria
 * - SLA por contrato/cliente
 */
@Getter
public enum TicketPriority {
    
    BAIXA("Baixa", Duration.ofHours(72), 1),
    MEDIA("Média", Duration.ofHours(48), 2),
    ALTA("Alta", Duration.ofHours(24), 3),
    CRITICA("Crítica", Duration.ofHours(4), 4);
    
    private final String label;
    private final Duration slaDuration;
    private final int weight;
    
    TicketPriority(String label, Duration slaDuration, int weight) {
        this.label = label;
        this.slaDuration = slaDuration;
        this.weight = weight;
    }
    
    /**
     * Retorna a duração do SLA em horas.
     */
    public long getSlaHours() {
        return slaDuration.toHours();
    }
    
    /**
     * Verifica se a prioridade é crítica (requer atenção imediata).
     */
    public boolean isCritical() {
        return this == CRITICA;
    }
    
    /**
     * Verifica se a prioridade requer atenção prioritária (ALTA ou CRITICA).
     */
    public boolean requiresPriorityAttention() {
        return this == ALTA || this == CRITICA;
    }
}
