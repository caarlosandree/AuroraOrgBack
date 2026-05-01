package br.com.api.auroraorg.ticket.permission;

import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Componente responsável por validar transições de status de chamados.
 * 
 * Implementa a matriz de transições permitidas conforme regras de negócio:
 * - ABERTO → EM_TRIAGEM | CANCELADO
 * - EM_TRIAGEM → EM_ATENDIMENTO | CANCELADO
 * - EM_ATENDIMENTO → AGUARDANDO_SOLICITANTE | RESOLVIDO | CANCELADO
 * - AGUARDANDO_SOLICITANTE → EM_ATENDIMENTO | CANCELADO
 * - RESOLVIDO → FECHADO | EM_ATENDIMENTO (reabertura)
 * - FECHADO → (terminal, sem transições)
 * - CANCELADO → (terminal, sem transições)
 * 
 * Esta abordagem com EnumMap oferece:
 * - O(1) lookup de transições permitidas
 * - Type safety em tempo de compilação
 * - Fácil manutenção e extensão
 */
@Component
public class TicketStatusTransition {

    private final Map<TicketStatus, Set<TicketStatus>> allowedTransitions;

    public TicketStatusTransition() {
        this.allowedTransitions = new EnumMap<>(TicketStatus.class);
        initializeTransitions();
    }

    /**
     * Inicializa a matriz de transições permitidas.
     */
    private void initializeTransitions() {
        // ABERTO: pode ir para triagem ou ser cancelado
        allowedTransitions.put(TicketStatus.ABERTO, Set.of(
            TicketStatus.EM_TRIAGEM,
            TicketStatus.CANCELADO
        ));

        // EM_TRIAGEM: pode ir para atendimento ou ser cancelado
        allowedTransitions.put(TicketStatus.EM_TRIAGEM, Set.of(
            TicketStatus.EM_ATENDIMENTO,
            TicketStatus.CANCELADO
        ));

        // EM_ATENDIMENTO: pode aguardar solicitante, resolver ou cancelar
        allowedTransitions.put(TicketStatus.EM_ATENDIMENTO, Set.of(
            TicketStatus.AGUARDANDO_SOLICITANTE,
            TicketStatus.RESOLVIDO,
            TicketStatus.CANCELADO
        ));

        // AGUARDANDO_SOLICITANTE: pode voltar para atendimento ou ser cancelado
        allowedTransitions.put(TicketStatus.AGUARDANDO_SOLICITANTE, Set.of(
            TicketStatus.EM_ATENDIMENTO,
            TicketStatus.CANCELADO
        ));

        // RESOLVIDO: pode ser fechado ou reaberto (voltar para atendimento)
        allowedTransitions.put(TicketStatus.RESOLVIDO, Set.of(
            TicketStatus.FECHADO,
            TicketStatus.EM_ATENDIMENTO  // Reabertura
        ));

        // FECHADO: status terminal (sem transições)
        allowedTransitions.put(TicketStatus.FECHADO, Set.of());

        // CANCELADO: status terminal (sem transições)
        allowedTransitions.put(TicketStatus.CANCELADO, Set.of());
    }

    /**
     * Verifica se uma transição de status é permitida.
     * 
     * @param from Status atual
     * @param to Status desejado
     * @return true se a transição é válida
     */
    public boolean canTransition(TicketStatus from, TicketStatus to) {
        if (from == null || to == null) {
            return false;
        }
        
        Set<TicketStatus> allowed = allowedTransitions.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Valida uma transição, lançando exceção se inválida.
     * 
     * @param from Status atual
     * @param to Status desejado
     * @throws InvalidStatusTransitionException se a transição for inválida
     */
    public void validateTransition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    /**
     * Retorna os status permitidos a partir de um status atual.
     * 
     * @param from Status atual
     * @return Conjunto de status permitidos (imutável)
     */
    public Set<TicketStatus> getAllowedTransitions(TicketStatus from) {
        return allowedTransitions.getOrDefault(from, Set.of());
    }

    /**
     * Verifica se um status é terminal (não permite transições).
     * 
     * @param status Status a verificar
     * @return true se for terminal
     */
    public boolean isTerminal(TicketStatus status) {
        return status == TicketStatus.FECHADO || status == TicketStatus.CANCELADO;
    }

    /**
     * Verifica se uma transição representa uma reabertura de chamado.
     * 
     * @param from Status atual
     * @param to Status desejado
     * @return true se for reabertura (RESOLVIDO → EM_ATENDIMENTO)
     */
    public boolean isReopening(TicketStatus from, TicketStatus to) {
        return from == TicketStatus.RESOLVIDO && to == TicketStatus.EM_ATENDIMENTO;
    }
}
