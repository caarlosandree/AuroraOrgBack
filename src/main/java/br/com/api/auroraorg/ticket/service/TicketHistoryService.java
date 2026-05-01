package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.TicketEventResponse;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.repository.TicketEventRepository;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de Eventos de Histórico de Chamados.
 *
 * Responsabilidades:
 * - Registrar eventos automáticos e manuais do ciclo de vida do chamado
 * - Listar eventos com filtros de visibilidade
 * - Fornecer dados para auditoria e timeline
 *
 * Eventos são imutáveis e representam ações importantes no sistema.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketEventRepository eventRepository;

    // ========== OPERAÇÕES DE REGISTRO DE EVENTOS ==========

    /**
     * Registra o evento de criação de chamado.
     */
    @Transactional
    public TicketEvent recordTicketCreated(Ticket ticket, User actor) {
        log.debug("Registrando evento de criação de chamado: {}", ticket.getId());

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CHAMADO_CRIADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(actor.getName() + " abriu o chamado")
                .description("Chamado criado com prioridade " + ticket.getPriority().getLabel())
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de alteração de status.
     */
    @Transactional
    public TicketEvent recordStatusChanged(Ticket ticket, User actor, TicketStatus oldStatus, TicketStatus newStatus) {
        log.debug("Registrando evento de alteração de status: {} -> {}", oldStatus, newStatus);

        String title = actor.getName() + " alterou o status para " + newStatus.getLabel();

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.STATUS_ALTERADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Status alterado de " + oldStatus.getLabel() + " para " + newStatus.getLabel())
                .oldValue(oldStatus.name())
                .newValue(newStatus.name())
                .metadata(Map.of(
                        "oldStatusLabel", oldStatus.getLabel(),
                        "newStatusLabel", newStatus.getLabel()
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de alteração de prioridade.
     */
    @Transactional
    public TicketEvent recordPriorityChanged(Ticket ticket, User actor, TicketPriority oldPriority, TicketPriority newPriority) {
        log.debug("Registrando evento de alteração de prioridade: {} -> {}", oldPriority, newPriority);

        String title = actor.getName() + " alterou a prioridade para " + newPriority.getLabel();

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.PRIORIDADE_ALTERADA)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Prioridade alterada de " + oldPriority.getLabel() + " para " + newPriority.getLabel())
                .oldValue(oldPriority.name())
                .newValue(newPriority.name())
                .metadata(Map.of(
                        "oldPriorityLabel", oldPriority.getLabel(),
                        "newPriorityLabel", newPriority.getLabel(),
                        "slaHours", newPriority.getSlaHours()
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de atribuição de responsável.
     */
    @Transactional
    public TicketEvent recordAssigneeAssigned(Ticket ticket, User actor, User assignee) {
        log.debug("Registrando evento de atribuição de responsável: {}", assignee.getEmail());

        String title = actor.getName() + " atribuiu o chamado a " + assignee.getName();

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.RESPONSAVEL_ATRIBUIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Responsável atribuído: " + assignee.getName())
                .newValue(assignee.getName())
                .metadata(Map.of(
                        "assigneeId", assignee.getId(),
                        "assigneeName", assignee.getName(),
                        "assigneeEmail", assignee.getEmail()
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de troca de responsável.
     */
    @Transactional
    public TicketEvent recordAssigneeChanged(Ticket ticket, User actor, User oldAssignee, User newAssignee) {
        log.debug("Registrando evento de troca de responsável: {} -> {}",
                oldAssignee.getName(), newAssignee.getName());

        String title = actor.getName() + " transferiu o chamado para " + newAssignee.getName();

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.RESPONSAVEL_ALTERADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Responsável alterado de " + oldAssignee.getName() + " para " + newAssignee.getName())
                .oldValue(oldAssignee.getName())
                .newValue(newAssignee.getName())
                .metadata(Map.of(
                        "oldAssigneeId", oldAssignee.getId(),
                        "oldAssigneeName", oldAssignee.getName(),
                        "newAssigneeId", newAssignee.getId(),
                        "newAssigneeName", newAssignee.getName()
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de remoção de responsável.
     */
    @Transactional
    public TicketEvent recordAssigneeRemoved(Ticket ticket, User actor, User oldAssignee) {
        log.debug("Registrando evento de remoção de responsável");

        String title = actor.getName() + " removeu o responsável do chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.RESPONSAVEL_REMOVIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Responsável " + oldAssignee.getName() + " removido do chamado")
                .oldValue(oldAssignee.getName())
                .visibility(CommentVisibility.INTERNO)
                .metadata(Map.of(
                        "oldAssigneeId", oldAssignee.getId(),
                        "oldAssigneeName", oldAssignee.getName()
                ))
                .build();

        return eventRepository.save(event);
    }

    /**
     * Alias em português para recordAssigneeAssigned.
     */
    @Transactional
    public TicketEvent recordResponsavelAtribuido(Ticket ticket, User actor, User assignee) {
        return recordAssigneeAssigned(ticket, actor, assignee);
    }

    /**
     * Alias em português para recordAssigneeChanged.
     */
    @Transactional
    public TicketEvent recordResponsavelAlterado(Ticket ticket, User actor, User oldAssignee, User newAssignee) {
        return recordAssigneeChanged(ticket, actor, oldAssignee, newAssignee);
    }

    /**
     * Alias em português para recordAssigneeRemoved.
     */
    @Transactional
    public TicketEvent recordResponsavelRemovido(Ticket ticket, User actor, User oldAssignee) {
        return recordAssigneeRemoved(ticket, actor, oldAssignee);
    }

    /**
     * Registra evento de alteração de fila.
     */
    @Transactional
    public TicketEvent recordFilaAlterada(Ticket ticket, User actor, String oldFilaName, String newFilaName) {
        log.debug("Registrando evento de alteração de fila: {} -> {}", oldFilaName, newFilaName);

        String title = actor.getName() + " alterou a fila do chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.FILA_ALTERADA)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Fila alterada de " + (oldFilaName != null ? oldFilaName : "nenhuma") + " para " + newFilaName)
                .oldValue(oldFilaName)
                .newValue(newFilaName)
                .metadata(Map.of(
                        "oldFilaName", oldFilaName != null ? oldFilaName : "nenhuma",
                        "newFilaName", newFilaName
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de alteração de categoria.
     */
    @Transactional
    public TicketEvent recordCategoriaAlterada(Ticket ticket, User actor, String oldCategoriaName, String newCategoriaName) {
        log.debug("Registrando evento de alteração de categoria: {} -> {}", oldCategoriaName, newCategoriaName);

        String title = actor.getName() + " alterou a categoria do chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CATEGORIA_ALTERADA)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Categoria alterada de " + (oldCategoriaName != null ? oldCategoriaName : "nenhuma") + " para " + newCategoriaName)
                .oldValue(oldCategoriaName)
                .newValue(newCategoriaName)
                .metadata(Map.of(
                        "oldCategoriaName", oldCategoriaName != null ? oldCategoriaName : "nenhuma",
                        "newCategoriaName", newCategoriaName
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de chamado assumido por agente.
     */
    @Transactional
    public TicketEvent recordChamadoAssumido(Ticket ticket, User actor) {
        log.debug("Registrando evento de chamado assumido: {}", actor.getEmail());

        String title = actor.getName() + " assumiu o chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CHAMADO_ASSUMIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("O agente " + actor.getName() + " assumiu o chamado")
                .newValue(actor.getName())
                .metadata(Map.of(
                        "agenteId", actor.getId(),
                        "agenteName", actor.getName(),
                        "agenteEmail", actor.getEmail()
                ))
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de adição de agente a uma fila.
     */
    @Transactional
    public TicketEvent recordAgenteAdicionadoFila(Ticket ticket, User actor, User agente, String filaName) {
        log.debug("Registrando evento de agente adicionado à fila: {} -> {}", agente.getName(), filaName);

        String title = actor.getName() + " adicionou " + agente.getName() + " à fila " + filaName;

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.AGENTE_ADICIONADO_FILA)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Agente " + agente.getName() + " vinculado à fila " + filaName)
                .newValue(agente.getName())
                .metadata(Map.of(
                        "agenteId", agente.getId(),
                        "agenteName", agente.getName(),
                        "filaName", filaName
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de remoção de agente de uma fila.
     */
    @Transactional
    public TicketEvent recordAgenteRemovidoFila(Ticket ticket, User actor, User agente, String filaName) {
        log.debug("Registrando evento de agente removido da fila: {} -> {}", agente.getName(), filaName);

        String title = actor.getName() + " removeu " + agente.getName() + " da fila " + filaName;

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.AGENTE_REMOVIDO_FILA)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Agente " + agente.getName() + " desvinculado da fila " + filaName)
                .oldValue(agente.getName())
                .metadata(Map.of(
                        "agenteId", agente.getId(),
                        "agenteName", agente.getName(),
                        "filaName", filaName
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de resolução de chamado.
     */
    @Transactional
    public TicketEvent recordTicketResolved(Ticket ticket, User actor) {
        log.debug("Registrando evento de resolução de chamado");

        String title = actor.getName() + " resolveu o chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CHAMADO_RESOLVIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Chamado marcado como resolvido")
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de fechamento de chamado.
     */
    @Transactional
    public TicketEvent recordTicketClosed(Ticket ticket, User actor) {
        log.debug("Registrando evento de fechamento de chamado");

        String title = actor.getName() + " fechou o chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CHAMADO_FECHADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Chamado encerrado definitivamente")
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de cancelamento de chamado.
     */
    @Transactional
    public TicketEvent recordTicketCancelled(Ticket ticket, User actor) {
        log.debug("Registrando evento de cancelamento de chamado");

        String title = actor.getName() + " cancelou o chamado";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.CHAMADO_CANCELADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Chamado cancelado")
                .visibility(CommentVisibility.PUBLICO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de SLA em risco (automático do sistema).
     */
    @Transactional
    public TicketEvent recordSlaAtRisk(Ticket ticket) {
        log.warn("Registrando evento de SLA em risco: {}", ticket.getId());

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.SLA_EM_RISCO)
                .origin(TicketEventOrigin.SISTEMA)
                .title("Sistema marcou o SLA como em risco")
                .description("O SLA do chamado está próximo do vencimento")
                .metadata(Map.of(
                        "slaDueAt", ticket.getSlaDueAt().toString(),
                        "remainingHours", ticket.getRemainingSlaHours()
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de SLA vencido (automático do sistema).
     */
    @Transactional
    public TicketEvent recordSlaOverdue(Ticket ticket) {
        log.error("Registrando evento de SLA vencido: {}", ticket.getId());

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.SLA_VENCIDO)
                .origin(TicketEventOrigin.SISTEMA)
                .title("Sistema marcou o SLA como vencido")
                .description("O SLA do chamado foi violado")
                .metadata(Map.of(
                        "slaDueAt", ticket.getSlaDueAt().toString()
                ))
                .visibility(CommentVisibility.INTERNO)
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de adição de comentário.
     */
    @Transactional
    public TicketEvent recordCommentAdded(Ticket ticket, User actor, TicketComment comment) {
        log.debug("Registrando evento de adição de comentário");

        String title = actor.getName() + " adicionou um comentário";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.COMENTARIO_ADICIONADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Novo comentário adicionado ao chamado")
                .visibility(comment.getVisibility())
                .metadata(Map.of(
                        "commentId", comment.getId(),
                        "visibility", comment.getVisibility().name()
                ))
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de edição de comentário.
     */
    @Transactional
    public TicketEvent recordCommentEdited(Ticket ticket, User actor, TicketComment comment) {
        log.debug("Registrando evento de edição de comentário");

        String title = actor.getName() + " editou um comentário";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.COMENTARIO_EDITADO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Comentário foi editado")
                .visibility(comment.getVisibility())
                .metadata(Map.of(
                        "commentId", comment.getId()
                ))
                .build();

        return eventRepository.save(event);
    }

    /**
     * Registra evento de remoção de comentário.
     */
    @Transactional
    public TicketEvent recordCommentRemoved(Ticket ticket, User actor, TicketComment comment) {
        log.debug("Registrando evento de remoção de comentário");

        String title = actor.getName() + " removeu um comentário";

        TicketEvent event = TicketEvent.builder()
                .ticket(ticket)
                .eventType(TicketEventType.COMENTARIO_REMOVIDO)
                .actor(actor)
                .origin(TicketEventOrigin.USUARIO)
                .title(title)
                .description("Comentário foi removido logicamente")
                .visibility(CommentVisibility.INTERNO)
                .metadata(Map.of(
                        "commentId", comment.getId(),
                        "originalVisibility", comment.getVisibility().name()
                ))
                .build();

        return eventRepository.save(event);
    }

    // ========== OPERAÇÕES DE CONSULTA ==========

    /**
     * Lista todos os eventos de um chamado (acesso administrativo).
     */
    @Transactional(readOnly = true)
    public List<TicketEventResponse> listAllEvents(UUID ticketId) {
        log.debug("Listando todos os eventos do chamado: {}", ticketId);

        return eventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos públicos de um chamado (visão do solicitante).
     */
    @Transactional(readOnly = true)
    public List<TicketEventResponse> listPublicEvents(UUID ticketId) {
        log.debug("Listando eventos públicos do chamado: {}", ticketId);

        return eventRepository.findPublicEventsByTicketId(ticketId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos visíveis para a equipe (públicos e internos).
     */
    @Transactional(readOnly = true)
    public List<TicketEventResponse> listAllVisibleEvents(UUID ticketId) {
        log.debug("Listando eventos visíveis (equipe) do chamado: {}", ticketId);

        return eventRepository.findAllVisibleEventsByTicketId(ticketId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista eventos visíveis para a equipe com paginação.
     */
    @Transactional(readOnly = true)
    public Page<TicketEventResponse> listAllVisibleEvents(UUID ticketId, Pageable pageable) {
        log.debug("Listando eventos visíveis paginados do chamado: {}", ticketId);

        return eventRepository.findAllVisibleEventsByTicketId(ticketId, pageable)
                .map(this::toResponse);
    }

    /**
     * Lista eventos conforme o perfil do usuário.
     */
    @Transactional(readOnly = true)
    public List<TicketEventResponse> listEventsForUser(UUID ticketId, UserRole role) {
        log.debug("Listando eventos do chamado {} para perfil {}", ticketId, role);

        if (role == UserRole.SOLICITANTE) {
            return listPublicEvents(ticketId);
        } else {
            // ADMIN, AGENTE, GESTOR veem todos os eventos visíveis
            return listAllVisibleEvents(ticketId);
        }
    }

    // ========== CONVERSÃO ==========

    private TicketEventResponse toResponse(TicketEvent event) {
        TicketEventResponse.ActorInfo actorInfo = null;
        if (event.getActor() != null) {
            actorInfo = new TicketEventResponse.ActorInfo(
                    event.getActor().getId(),
                    event.getActor().getName(),
                    event.getActor().getEmail()
            );
        }

        return new TicketEventResponse(
                event.getId(),
                event.getTicket().getId(),
                event.getEventType(),
                event.getEventType().getLabel(),
                event.getOrigin(),
                actorInfo,
                event.getTitle(),
                event.getDescription(),
                event.getOldValue(),
                event.getNewValue(),
                event.getMetadata(),
                event.getVisibility(),
                event.getCreatedAt()
        );
    }
}
