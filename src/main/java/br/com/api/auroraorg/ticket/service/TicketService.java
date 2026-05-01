package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.mapper.TicketMapper;
import br.com.api.auroraorg.ticket.permission.TicketStatusTransition;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.repository.TicketSpecifications;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.ticket.util.SlaCalculator;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service de Chamados (Tickets) com todas as regras de negócio.
 * 
 * Regras implementadas:
 * - Criação: qualquer usuário autenticado, status ABERTO, solicitante automático
 * - Atribuição: apenas ADMIN/AGENTE/GESTOR
 * - Status: apenas ADMIN/AGENTE/GESTOR alteram status operacionais
 * - Cancelamento: solicitante pode cancelar próprio chamado (se não finalizado)
 * - Resolução: ADMIN/AGENTE/GESTOR ou responsável atual
 * - Fechamento: apenas chamados RESOLVIDOS podem ser fechados
 * - Transições: validadas pela matriz TicketStatusTransition
 * - SLA: calculado automaticamente na criação/atualização de prioridade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final TicketStatusTransition statusTransition;
    private final SecurityUtils securityUtils;
    private final SlaCalculator slaCalculator;

    // ========== OPERAÇÕES CRUD ==========

    /**
     * Cria um novo chamado.
     * 
     * Regras:
     * - Qualquer usuário autenticado pode criar
     * - Status inicia como ABERTO automaticamente
     * - Solicitante é o usuário autenticado
     * - Responsável inicia nulo
     * - SLA calculado pela prioridade
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        
        log.info("Criando chamado. Solicitante: {}, Prioridade: {}", 
                currentUser.getEmail(), request.priority());

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setRequester(currentUser);
        ticket.setSlaDueAt(slaCalculator.calculateDueDate(request.priority()));

        Ticket saved = ticketRepository.save(ticket);
        
        log.info("Chamado criado com sucesso. ID: {}, SLA vencimento: {}", 
                saved.getId(), saved.getSlaDueAt());
        
        return ticketMapper.toResponse(saved);
    }

    /**
     * Busca chamado por ID.
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        return ticketMapper.toResponse(ticket);
    }

    /**
     * Lista chamados com filtros e paginação.
     */
    @Transactional(readOnly = true)
    public TicketListResponse listTickets(TicketFilterRequest filters, Pageable pageable) {
        Page<Ticket> tickets;
        
        if (hasActiveFilters(filters)) {
            tickets = ticketRepository.findAll(TicketSpecifications.withFilters(filters), pageable);
        } else {
            tickets = ticketRepository.findAll(pageable);
        }
        
        Page<TicketSummaryResponse> summaries = tickets.map(ticketMapper::toSummary);
        return new TicketListResponse(summaries);
    }

    /**
     * Atualiza dados básicos do chamado.
     * 
     * Regras:
     * - Não permite alterar chamado FECHADO ou CANCELADO
     * - Solicitante pode alterar próprio chamado (se aberto)
     * - ADMIN/AGENTE/GESTOR podem alterar qualquer chamado aberto
     */
    @Transactional
    public TicketResponse updateTicket(UUID id, UpdateTicketRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyTicket(ticket, currentUser, "atualizar");

        log.info("Atualizando chamado {}. Usuário: {}", id, currentUser.getEmail());

        ticketMapper.updateEntity(ticket, request);
        Ticket updated = ticketRepository.save(ticket);
        
        return ticketMapper.toResponse(updated);
    }

    // ========== OPERAÇÕES DE STATUS ==========

    /**
     * Altera status do chamado.
     * 
     * Regras:
     * - Valida transição pela matriz
     * - Atualiza datas conforme status (ex: resolvedAt)
     * - Apenas ADMIN/AGENTE/GESTOR podem alterar status operacionais
     */
    @Transactional
    public TicketResponse updateStatus(UUID id, UpdateStatusRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        TicketStatus newStatus = request.status();
        TicketStatus currentStatus = ticket.getStatus();

        // Valida transição
        statusTransition.validateTransition(currentStatus, newStatus);

        // Valida permissões para alteração de status
        validateStatusChangePermission(ticket, currentUser, newStatus);

        log.info("Alterando status do chamado {}: {} -> {}. Usuário: {}",
                id, currentStatus, newStatus, currentUser.getEmail());

        // Atualiza status e datas relacionadas
        updateStatusAndTimestamps(ticket, newStatus);

        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    // ========== OPERAÇÕES DE ATRIBUIÇÃO ==========

    /**
     * Atribui um responsável ao chamado.
     * 
     * Regras:
     * - Apenas ADMIN ou GESTOR podem atribuir a outro usuário
     * - Responsável deve ter perfil AGENTE ou ADMIN
     * - Não permite atribuir a chamados FECHADOS ou CANCELADOS
     */
    @Transactional
    public TicketResponse assignTicket(UUID id, AssignRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
            throw new TicketPermissionDeniedException("atribuir chamado a outro usuário", 
                    "Apenas ADMIN ou GESTOR podem atribuir chamados");
        }

        if (ticket.getStatus().isTerminal()) {
            throw new InvalidTicketOperationException(ticket.getStatus(), "atribuir responsável");
        }

        User newAssignee = userRepository.findById(request.assigneeId())
                .orElseThrow(() -> new br.com.api.auroraorg.shared.exception.InvalidCredentialsException());

        // Valida se o responsável pode atender chamados
        if (!canAttendTickets(newAssignee)) {
            throw new InvalidTicketOperationException("O usuário atribuído deve ter perfil AGENTE, ADMIN ou GESTOR");
        }

        log.info("Atribuindo chamado {} ao usuário {}. Responsável anterior: {}",
                id, newAssignee.getEmail(), 
                ticket.hasAssignee() ? ticket.getAssignee().getEmail() : "nenhum");

        ticket.setAssignee(newAssignee);
        
        // Se estava ABERTO, muda para EM_TRIAGEM
        if (ticket.getStatus() == TicketStatus.ABERTO) {
            ticket.setStatus(TicketStatus.EM_TRIAGEM);
        }

        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    /**
     * Usuário AGENTE assume o chamado.
     * 
     * Regras:
     * - Apenas AGENTE, ADMIN ou GESTOR podem assumir
     * - O usuário se torna o responsável
     */
    @Transactional
    public TicketResponse assumeTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        if (!canAttendTickets(currentUser)) {
            throw new TicketPermissionDeniedException("assumir chamado", 
                    "Apenas AGENTE, ADMIN ou GESTOR podem assumir chamados");
        }

        if (ticket.getStatus().isTerminal()) {
            throw new InvalidTicketOperationException(ticket.getStatus(), "assumir");
        }

        log.info("Usuário {} assumindo chamado {}", currentUser.getEmail(), id);

        ticket.setAssignee(currentUser);
        
        // Se estava ABERTO ou EM_TRIAGEM, muda para EM_ATENDIMENTO
        if (ticket.getStatus() == TicketStatus.ABERTO || ticket.getStatus() == TicketStatus.EM_TRIAGEM) {
            ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        }

        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    // ========== OPERAÇÕES DE FINALIZAÇÃO ==========

    /**
     * Cancela um chamado.
     * 
     * Regras:
     * - Solicitante pode cancelar próprio chamado (se não RESOLVIDO/FECHADO/CANCELADO)
     * - ADMIN/AGENTE/GESTOR podem cancelar qualquer chamado não finalizado
     */
    @Transactional
    public TicketResponse cancelTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        // Valida se pode cancelar
        if (!ticket.getStatus().canBeCancelledByRequester()) {
            throw new InvalidTicketOperationException("Não é possível cancelar chamado com status " + 
                    ticket.getStatus().getLabel());
        }

        // Solicitante só pode cancelar próprio chamado
        boolean isRequester = ticket.isRequestedBy(currentUser);
        boolean canCancelAny = securityUtils.canAttendTickets();

        if (!isRequester && !canCancelAny) {
            throw new TicketPermissionDeniedException("cancelar este chamado", 
                    "Você só pode cancelar seus próprios chamados");
        }

        log.info("Cancelando chamado {}. Usuário: {}", id, currentUser.getEmail());

        ticket.cancel();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    /**
     * Marca chamado como RESOLVIDO.
     * 
     * Regras:
     * - Apenas ADMIN/AGENTE/GESTOR ou o responsável atual podem resolver
     * - Define resolvedAt automaticamente
     */
    @Transactional
    public TicketResponse resolveTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanResolve(ticket, currentUser);

        log.info("Resolvendo chamado {}. Usuário: {}", id, currentUser.getEmail());

        // Valida transição (ex: EM_ATENDIMENTO -> RESOLVIDO)
        statusTransition.validateTransition(ticket.getStatus(), TicketStatus.RESOLVIDO);

        ticket.markAsResolved();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    /**
     * Fecha um chamado RESOLVIDO.
     * 
     * Regras:
     * - Apenas chamados RESOLVIDOS podem ser fechados
     * - ADMIN/AGENTE/GESTOR ou solicitante podem fechar
     */
    @Transactional
    public TicketResponse closeTicket(UUID id) {
        Ticket ticket = findTicketOrThrow(id);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        if (ticket.getStatus() != TicketStatus.RESOLVIDO) {
            throw new InvalidTicketOperationException(
                    "Apenas chamados RESOLVIDOS podem ser fechados. Status atual: " + 
                    ticket.getStatus().getLabel());
        }

        boolean isRequester = ticket.isRequestedBy(currentUser);
        boolean canClose = securityUtils.canAttendTickets() || isRequester;

        if (!canClose) {
            throw new TicketPermissionDeniedException("fechar este chamado");
        }

        log.info("Fechando chamado {}. Usuário: {}", id, currentUser.getEmail());

        ticket.close();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private boolean hasActiveFilters(TicketFilterRequest filters) {
        if (filters == null) return false;
        return filters.status() != null ||
               filters.priority() != null ||
               filters.category() != null ||
               filters.requesterId() != null ||
               filters.assigneeId() != null ||
               filters.slaOverdue() != null ||
               (filters.searchTerm() != null && !filters.searchTerm().isBlank());
    }

    private boolean canAttendTickets(User user) {
        return user.getRole() == UserRole.ADMIN ||
               user.getRole() == UserRole.AGENTE ||
               user.getRole() == UserRole.GESTOR;
    }

    private void validateCanModifyTicket(Ticket ticket, User user, String operation) {
        // Chamados FECHADOS ou CANCELADOS não podem ser alterados
        if (ticket.getStatus().isTerminal()) {
            throw new InvalidTicketOperationException(ticket.getStatus(), operation);
        }

        boolean isRequester = ticket.isRequestedBy(user);
        boolean canModifyAny = securityUtils.canAttendTickets();

        // Solicitante só pode alterar próprio chamado em status iniciais
        if (isRequester && !canModifyAny) {
            if (ticket.getStatus() != TicketStatus.ABERTO) {
                throw new TicketPermissionDeniedException(operation + " este chamado",
                        "Chamado já está em atendimento");
            }
            return; // Permitido - solicitante alterando chamado ABERTO próprio
        }

        if (!canModifyAny && !isRequester) {
            throw new TicketPermissionDeniedException(operation + " este chamado");
        }
    }

    private void validateStatusChangePermission(Ticket ticket, User user, TicketStatus newStatus) {
        // Transições para status operacionais exigem permissão de atendente
        boolean isOperationalTransition = newStatus == TicketStatus.EM_TRIAGEM ||
                                          newStatus == TicketStatus.EM_ATENDIMENTO ||
                                          newStatus == TicketStatus.AGUARDANDO_SOLICITANTE ||
                                          newStatus == TicketStatus.RESOLVIDO;

        if (isOperationalTransition && !securityUtils.canAttendTickets()) {
            throw new TicketPermissionDeniedException("alterar para status " + newStatus.getLabel(),
                    "Apenas AGENTE, ADMIN ou GESTOR podem alterar status operacionais");
        }

        // Cancelamento tem regra específica no método cancelTicket
        // Fechamento tem regra específica no método closeTicket
    }

    private void validateCanResolve(Ticket ticket, User user) {
        if (ticket.getStatus().isTerminal()) {
            throw new InvalidTicketOperationException(ticket.getStatus(), "resolver");
        }

        boolean isAssignee = ticket.isAssignedTo(user);
        boolean canResolve = securityUtils.canAttendTickets();

        if (!canResolve && !isAssignee) {
            throw new TicketPermissionDeniedException("resolver este chamado",
                    "Apenas ADMIN, AGENTE, GESTOR ou o responsável atual podem resolver");
        }
    }

    private void updateStatusAndTimestamps(Ticket ticket, TicketStatus newStatus) {
        TicketStatus oldStatus = ticket.getStatus();

        switch (newStatus) {
            case RESOLVIDO:
                ticket.markAsResolved();
                break;
            case FECHADO:
                ticket.close();
                break;
            case CANCELADO:
                ticket.cancel();
                break;
            case EM_ATENDIMENTO:
                // Se estava RESOLVIDO, é uma reabertura
                if (oldStatus == TicketStatus.RESOLVIDO) {
                    ticket.reopen();
                } else {
                    ticket.setStatus(newStatus);
                }
                break;
            default:
                ticket.setStatus(newStatus);
        }
    }
}
