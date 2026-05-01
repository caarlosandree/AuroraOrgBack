package br.com.api.auroraorg.ticket.util;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Calculadora de SLA (Service Level Agreement) para chamados.
 * 
 * Regras atuais (simples):
 * - BAIXA: agora + 72 horas
 * - MEDIA: agora + 48 horas
 * - ALTA: agora + 24 horas
 * - CRITICA: agora + 4 horas
 * 
 * Melhorias futuras planejadas:
 * 1. Calendário comercial: considerar apenas dias úteis (seg-sex)
 * 2. Horários de atendimento: SLA contabiliza apenas dentro do expediente
 * 3. Feriados: excluir feriados nacionais e regionais
 * 4. SLA por categoria: diferentes SLAs para diferentes tipos de chamado
 * 5. SLA por cliente: contratos com SLAs diferenciados
 * 6. SLA em horas úteis vs 24x7
 * 7. Alertas de SLA: notificações quando 50%, 75%, 90% do SLA consumido
 * 8. SLA de resposta vs SLA de resolução
 */
@Slf4j
@Component
public class SlaCalculator {

    /**
     * Calcula a data/hora de vencimento do SLA baseada na prioridade.
     * 
     * @param priority Prioridade do chamado
     * @return Data/hora limite do SLA
     */
    public LocalDateTime calculateDueDate(TicketPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plus(priority.getSlaDuration());
        
        log.debug("SLA calculado: prioridade={}, duracao={}h, vencimento={}",
                priority, priority.getSlaHours(), dueDate);
        
        return dueDate;
    }

    /**
     * Calcula o SLA padrão (para quando prioridade não é informada).
     * Usa prioridade MEDIA (48 horas).
     */
    public LocalDateTime calculateDefaultDueDate() {
        return calculateDueDate(TicketPriority.MEDIA);
    }

    /**
     * Recalcula o SLA quando a prioridade é alterada.
     * Mantém a data de criação como base e recalcula a partir dela.
     * 
     * @param priority Nova prioridade
     * @param createdAt Data de criação original
     * @return Nova data/hora limite do SLA
     */
    public LocalDateTime recalculateDueDate(TicketPriority priority, LocalDateTime createdAt) {
        LocalDateTime dueDate = createdAt.plus(priority.getSlaDuration());
        
        log.debug("SLA recalculado: prioridade={}, criadoEm={}, novoVencimento={}",
                priority, createdAt, dueDate);
        
        return dueDate;
    }

    /**
     * Verifica se o SLA está vencido.
     * 
     * @param slaDueAt Data/hora limite do SLA
     * @return true se vencido
     */
    public boolean isOverdue(LocalDateTime slaDueAt) {
        return LocalDateTime.now().isAfter(slaDueAt);
    }

    /**
     * Calcula o percentual de SLA consumido.
     * 
     * @param createdAt Data de criação
     * @param slaDueAt Data limite do SLA
     * @return Percentual consumido (0-100+)
     */
    public double calculateConsumedPercentage(LocalDateTime createdAt, LocalDateTime slaDueAt) {
        java.time.Duration totalDuration = java.time.Duration.between(createdAt, slaDueAt);
        java.time.Duration elapsedDuration = java.time.Duration.between(createdAt, LocalDateTime.now());
        
        if (totalDuration.isZero() || totalDuration.isNegative()) {
            return 100.0;
        }
        
        double percentage = (elapsedDuration.toMillis() * 100.0) / totalDuration.toMillis();
        return Math.min(percentage, 100.0);
    }

    /**
     * Calcula as horas restantes do SLA.
     * 
     * @param slaDueAt Data limite do SLA
     * @return Horas restantes (0 se vencido)
     */
    public long getRemainingHours(LocalDateTime slaDueAt) {
        if (isOverdue(slaDueAt)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), slaDueAt).toHours();
    }

    /**
     * Determina a classe de SLA para fins de UI/alerta.
     * 
     * @param createdAt Data de criação
     * @param slaDueAt Data limite do SLA
     * @return Classe do SLA: NORMAL, WARNING, CRITICAL, OVERDUE
     */
    public SlaClass classifySla(LocalDateTime createdAt, LocalDateTime slaDueAt) {
        if (isOverdue(slaDueAt)) {
            return SlaClass.OVERDUE;
        }
        
        double consumed = calculateConsumedPercentage(createdAt, slaDueAt);
        
        if (consumed >= 90) {
            return SlaClass.CRITICAL;
        } else if (consumed >= 75) {
            return SlaClass.WARNING;
        }
        
        return SlaClass.NORMAL;
    }

    /**
     * Enum para classificação visual do SLA.
     */
    public enum SlaClass {
        NORMAL,     // Verde - dentro do esperado
        WARNING,    // Amarelo - 75% consumido
        CRITICAL,   // Laranja - 90% consumido
        OVERDUE     // Vermelho - vencido
    }
}
