package br.com.api.auroraorg.ticket.util;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para SlaCalculator.
 */
class SlaCalculatorTest {

    private SlaCalculator slaCalculator;

    @BeforeEach
    void setUp() {
        slaCalculator = new SlaCalculator();
    }

    @Test
    @DisplayName("Deve calcular SLA corretamente para prioridade BAIXA (72h)")
    void shouldCalculateSlaForBaixaPriority() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = slaCalculator.calculateDueDate(TicketPriority.BAIXA);
        
        assertThat(due).isAfter(now.plusHours(71));
        assertThat(due).isBefore(now.plusHours(73));
    }

    @Test
    @DisplayName("Deve calcular SLA corretamente para prioridade MEDIA (48h)")
    void shouldCalculateSlaForMediaPriority() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = slaCalculator.calculateDueDate(TicketPriority.MEDIA);
        
        assertThat(due).isAfter(now.plusHours(47));
        assertThat(due).isBefore(now.plusHours(49));
    }

    @Test
    @DisplayName("Deve calcular SLA corretamente para prioridade ALTA (24h)")
    void shouldCalculateSlaForAltaPriority() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = slaCalculator.calculateDueDate(TicketPriority.ALTA);
        
        assertThat(due).isAfter(now.plusHours(23));
        assertThat(due).isBefore(now.plusHours(25));
    }

    @Test
    @DisplayName("Deve calcular SLA corretamente para prioridade CRITICA (4h)")
    void shouldCalculateSlaForCriticaPriority() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = slaCalculator.calculateDueDate(TicketPriority.CRITICA);
        
        assertThat(due).isAfter(now.plusHours(3));
        assertThat(due).isBefore(now.plusHours(5));
    }

    @Test
    @DisplayName("Deve detectar SLA vencido")
    void shouldDetectOverdueSla() {
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        assertThat(slaCalculator.isOverdue(past)).isTrue();
    }

    @Test
    @DisplayName("Deve detectar SLA não vencido")
    void shouldDetectNonOverdueSla() {
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        assertThat(slaCalculator.isOverdue(future)).isFalse();
    }

    @Test
    @DisplayName("Deve calcular horas restantes corretamente")
    void shouldCalculateRemainingHours() {
        LocalDateTime future = LocalDateTime.now().plusHours(10);
        long remaining = slaCalculator.getRemainingHours(future);
        
        assertThat(remaining).isCloseTo(10, org.assertj.core.data.Offset.offset(1L));
    }

    @Test
    @DisplayName("Horas restantes deve ser 0 quando SLA vencido")
    void shouldReturnZeroHoursWhenOverdue() {
        LocalDateTime past = LocalDateTime.now().minusHours(5);
        long remaining = slaCalculator.getRemainingHours(past);
        
        assertThat(remaining).isZero();
    }

    @Test
    @DisplayName("Deve recalcular SLA quando prioridade alterada")
    void shouldRecalculateSlaWhenPriorityChanged() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(12);
        LocalDateTime newDue = slaCalculator.recalculateDueDate(TicketPriority.ALTA, createdAt);
        LocalDateTime expected = createdAt.plusHours(24);
        
        assertThat(newDue).isAfter(expected.minusMinutes(1));
        assertThat(newDue).isBefore(expected.plusMinutes(1));
    }

    @Test
    @DisplayName("Deve classificar SLA como NORMAL quando baixo consumo")
    void shouldClassifyAsNormal() {
        LocalDateTime created = LocalDateTime.now().minusHours(10);
        LocalDateTime due = LocalDateTime.now().plusHours(40); // 20% consumido
        
        SlaCalculator.SlaClass classification = slaCalculator.classifySla(created, due);
        
        assertThat(classification).isEqualTo(SlaCalculator.SlaClass.NORMAL);
    }

    @Test
    @DisplayName("Deve classificar SLA como WARNING quando 75%+ consumido")
    void shouldClassifyAsWarning() {
        LocalDateTime created = LocalDateTime.now().minusHours(38);
        LocalDateTime due = LocalDateTime.now().plusHours(10); // ~79% consumido
        
        SlaCalculator.SlaClass classification = slaCalculator.classifySla(created, due);
        
        assertThat(classification).isEqualTo(SlaCalculator.SlaClass.WARNING);
    }

    @Test
    @DisplayName("Deve classificar SLA como CRITICAL quando 90%+ consumido")
    void shouldClassifyAsCritical() {
        LocalDateTime created = LocalDateTime.now().minusHours(46);
        LocalDateTime due = LocalDateTime.now().plusHours(2); // ~96% consumido
        
        SlaCalculator.SlaClass classification = slaCalculator.classifySla(created, due);
        
        assertThat(classification).isEqualTo(SlaCalculator.SlaClass.CRITICAL);
    }

    @Test
    @DisplayName("Deve classificar SLA como OVERDUE quando vencido")
    void shouldClassifyAsOverdue() {
        LocalDateTime created = LocalDateTime.now().minusHours(50);
        LocalDateTime due = LocalDateTime.now().minusHours(2); // Vencido
        
        SlaCalculator.SlaClass classification = slaCalculator.classifySla(created, due);
        
        assertThat(classification).isEqualTo(SlaCalculator.SlaClass.OVERDUE);
    }
}
