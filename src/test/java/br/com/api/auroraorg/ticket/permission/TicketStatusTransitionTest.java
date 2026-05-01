package br.com.api.auroraorg.ticket.permission;

import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para TicketStatusTransition.
 * 
 * Valida a matriz completa de transições permitidas.
 */
class TicketStatusTransitionTest {

    private TicketStatusTransition transition;

    @BeforeEach
    void setUp() {
        transition = new TicketStatusTransition();
    }

    // ========== TRANSIÇÕES PERMITIDAS ==========

    @ParameterizedTest
    @CsvSource({
        "ABERTO, EM_TRIAGEM",
        "ABERTO, CANCELADO",
        "EM_TRIAGEM, EM_ATENDIMENTO",
        "EM_TRIAGEM, CANCELADO",
        "EM_ATENDIMENTO, AGUARDANDO_SOLICITANTE",
        "EM_ATENDIMENTO, RESOLVIDO",
        "EM_ATENDIMENTO, CANCELADO",
        "AGUARDANDO_SOLICITANTE, EM_ATENDIMENTO",
        "AGUARDANDO_SOLICITANTE, CANCELADO",
        "RESOLVIDO, FECHADO",
        "RESOLVIDO, EM_ATENDIMENTO"
    })
    @DisplayName("Deve permitir transições válidas: {0} -> {1}")
    void shouldAllowValidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(transition.canTransition(from, to)).isTrue();
    }

    // ========== TRANSIÇÕES BLOQUEADAS ==========

    @ParameterizedTest
    @CsvSource({
        "ABERTO, ABERTO",
        "ABERTO, RESOLVIDO",
        "ABERTO, FECHADO",
        "EM_TRIAGEM, ABERTO",
        "EM_TRIAGEM, RESOLVIDO",
        "EM_ATENDIMENTO, ABERTO",
        "EM_ATENDIMENTO, FECHADO",
        "RESOLVIDO, ABERTO",
        "RESOLVIDO, CANCELADO",
        "FECHADO, ABERTO",
        "FECHADO, EM_ATENDIMENTO",
        "CANCELADO, ABERTO",
        "CANCELADO, RESOLVIDO"
    })
    @DisplayName("Deve bloquear transições inválidas: {0} -> {1}")
    void shouldBlockInvalidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(transition.canTransition(from, to)).isFalse();
    }

    // ========== STATUS TERMINAIS ==========

    @Test
    @DisplayName("FECHADO é status terminal sem transições permitidas")
    void fechadoIsTerminalStatus() {
        Set<TicketStatus> allowed = transition.getAllowedTransitions(TicketStatus.FECHADO);
        assertThat(allowed).isEmpty();
        assertThat(transition.isTerminal(TicketStatus.FECHADO)).isTrue();
    }

    @Test
    @DisplayName("CANCELADO é status terminal sem transições permitidas")
    void canceladoIsTerminalStatus() {
        Set<TicketStatus> allowed = transition.getAllowedTransitions(TicketStatus.CANCELADO);
        assertThat(allowed).isEmpty();
        assertThat(transition.isTerminal(TicketStatus.CANCELADO)).isTrue();
    }

    // ========== TRANSIÇÕES DE STATUS ABERTO ==========

    @Test
    @DisplayName("ABERTO permite transição para EM_TRIAGEM e CANCELADO")
    void abertoAllowsTransitionToTriagemAndCancelado() {
        Set<TicketStatus> allowed = transition.getAllowedTransitions(TicketStatus.ABERTO);
        assertThat(allowed).containsExactlyInAnyOrder(TicketStatus.EM_TRIAGEM, TicketStatus.CANCELADO);
    }

    // ========== TRANSIÇÕES DE STATUS EM_ATENDIMENTO ==========

    @Test
    @DisplayName("EM_ATENDIMENTO permite 3 transições")
    void emAtendimentoAllowsThreeTransitions() {
        Set<TicketStatus> allowed = transition.getAllowedTransitions(TicketStatus.EM_ATENDIMENTO);
        assertThat(allowed).containsExactlyInAnyOrder(
                TicketStatus.AGUARDANDO_SOLICITANTE,
                TicketStatus.RESOLVIDO,
                TicketStatus.CANCELADO
        );
    }

    // ========== TRANSIÇÕES DE STATUS RESOLVIDO ==========

    @Test
    @DisplayName("RESOLVIDO permite transição para FECHADO (encerrar) ou EM_ATENDIMENTO (reabrir)")
    void resolvidoAllowsCloseOrReopen() {
        Set<TicketStatus> allowed = transition.getAllowedTransitions(TicketStatus.RESOLVIDO);
        assertThat(allowed).containsExactlyInAnyOrder(
                TicketStatus.FECHADO,
                TicketStatus.EM_ATENDIMENTO
        );
    }

    @Test
    @DisplayName("RESOLVIDO -> EM_ATENDIMENTO é considerado reabertura")
    void resolvidoToEmAtendimentoIsReopening() {
        assertThat(transition.isReopening(TicketStatus.RESOLVIDO, TicketStatus.EM_ATENDIMENTO)).isTrue();
    }

    @Test
    @DisplayName("Outras transições para EM_ATENDIMENTO não são reabertura")
    void otherTransitionsToEmAtendimentoAreNotReopening() {
        assertThat(transition.isReopening(TicketStatus.AGUARDANDO_SOLICITANTE, TicketStatus.EM_ATENDIMENTO)).isFalse();
        assertThat(transition.isReopening(TicketStatus.EM_TRIAGEM, TicketStatus.EM_ATENDIMENTO)).isFalse();
    }

    // ========== VALIDAÇÃO COM EXCEPTION ==========

    @Test
    @DisplayName("validateTransition deve lançar exceção para transição inválida")
    void validateTransitionShouldThrowForInvalidTransition() {
        assertThatThrownBy(() -> transition.validateTransition(TicketStatus.FECHADO, TicketStatus.ABERTO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Transição de status inválida")
                .hasMessageContaining("Fechado")
                .hasMessageContaining("Aberto");
    }

    @Test
    @DisplayName("validateTransition não deve lançar exceção para transição válida")
    void validateTransitionShouldNotThrowForValidTransition() {
        // Não deve lançar exceção
        transition.validateTransition(TicketStatus.ABERTO, TicketStatus.EM_TRIAGEM);
    }

    // ========== CASOS DE BORDA ==========

    @Test
    @DisplayName("Transição com status null deve retornar false")
    void transitionWithNullStatusShouldReturnFalse() {
        assertThat(transition.canTransition(null, TicketStatus.ABERTO)).isFalse();
        assertThat(transition.canTransition(TicketStatus.ABERTO, null)).isFalse();
        assertThat(transition.canTransition(null, null)).isFalse();
    }

    @Test
    @DisplayName("Status terminal é detectado corretamente")
    void terminalStatusIsDetectedCorrectly() {
        assertThat(transition.isTerminal(TicketStatus.FECHADO)).isTrue();
        assertThat(transition.isTerminal(TicketStatus.CANCELADO)).isTrue();
        assertThat(transition.isTerminal(TicketStatus.RESOLVIDO)).isFalse();
        assertThat(transition.isTerminal(TicketStatus.ABERTO)).isFalse();
    }
}
