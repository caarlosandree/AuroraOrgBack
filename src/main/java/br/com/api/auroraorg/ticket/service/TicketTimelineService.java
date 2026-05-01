package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.CommentResponse;
import br.com.api.auroraorg.ticket.dto.TicketEventResponse;
import br.com.api.auroraorg.ticket.dto.TimelineItemResponse;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.ticket.exception.TicketNotFoundException;
import br.com.api.auroraorg.ticket.exception.TicketPermissionDeniedException;
import br.com.api.auroraorg.ticket.repository.TicketCommentRepository;
import br.com.api.auroraorg.ticket.repository.TicketEventRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para montagem da Timeline unificada de Chamados.
 *
 * A timeline combina:
 * - Comentários de usuários (podem ser editados/removidos logicamente)
 * - Eventos de histórico (imutáveis, registros de auditoria)
 *
 * Ordenação: cronológica (do mais antigo para o mais recente)
 *
 * Filtros por perfil:
 * - SOLICITANTE: apenas itens públicos
 * - AGENTE/ADMIN/GESTOR: itens públicos e internos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTimelineService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketEventRepository eventRepository;
    private final SecurityUtils securityUtils;

    /**
     * Obtém a timeline completa de um chamado.
     *
     * Combina comentários e eventos em uma lista única ordenada cronologicamente.
     * O filtro de visibilidade é aplicado conforme o perfil do usuário.
     *
     * @param ticketId ID do chamado
     * @return Lista de itens da timeline (comentários e eventos unificados)
     */
    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimeline(UUID ticketId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);

        log.debug("Obtendo timeline do chamado {}. Usuário: {}, Perfil: {}",
                ticketId, currentUser.getEmail(), currentUser.getRole());

        // Valida acesso ao chamado
        validateTicketAccess(ticket, currentUser);

        // Busca comentários e eventos conforme o perfil
        List<TimelineItemResponse> items = new ArrayList<>();

        if (currentUser.getRole() == UserRole.SOLICITANTE) {
            // Solicitante: apenas comentários e eventos públicos
            items.addAll(getPublicComments(ticketId));
            items.addAll(getPublicEvents(ticketId));
        } else {
            // Equipe (AGENTE, ADMIN, GESTOR): todos os itens visíveis
            items.addAll(getAllVisibleComments(ticketId));
            items.addAll(getAllVisibleEvents(ticketId));
        }

        // Ordena cronologicamente (do mais antigo para o mais recente)
        items.sort(Comparator.comparing(TimelineItemResponse::createdAt));

        log.debug("Timeline gerada com {} itens", items.size());

        return items;
    }

    /**
     * Obtém a timeline com paginação.
     */
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getTimeline(UUID ticketId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);

        log.debug("Obtendo timeline paginada do chamado {}. Página: {}, Tamanho: {}",
                ticketId, pageable.getPageNumber(), pageable.getPageSize());

        // Valida acesso
        validateTicketAccess(ticket, currentUser);

        // Busca todos os itens
        List<TimelineItemResponse> allItems = getTimeline(ticketId);

        // Aplica paginação manual
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allItems.size());

        if (start > allItems.size()) {
            return new PageImpl<>(List.of(), pageable, allItems.size());
        }

        List<TimelineItemResponse> pageContent = allItems.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allItems.size());
    }

    /**
     * Obtém a timeline filtrada por tipo.
     *
     * @param ticketId ID do chamado
     * @param type Tipo de item (COMENTARIO ou EVENTO), null para todos
     * @return Lista filtrada de itens
     */
    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimelineByType(UUID ticketId, TimelineItemResponse.TimelineItemType type) {
        List<TimelineItemResponse> allItems = getTimeline(ticketId);

        if (type == null) {
            return allItems;
        }

        return allItems.stream()
                .filter(item -> item.type() == type)
                .collect(Collectors.toList());
    }

    /**
     * Obtém a timeline em ordem reversa (mais recentes primeiro).
     * Útil para notificações e "últimas atividades".
     */
    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimelineReversed(UUID ticketId) {
        List<TimelineItemResponse> items = getTimeline(ticketId);
        return items.stream()
                .sorted(Comparator.comparing(TimelineItemResponse::createdAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Obtém os últimos N itens da timeline.
     */
    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getRecentTimelineItems(UUID ticketId, int count) {
        return getTimelineReversed(ticketId).stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Obtém itens da timeline desde uma data específica.
     * Útil para polling de atualizações.
     */
    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimelineSince(UUID ticketId, LocalDateTime since) {
        return getTimeline(ticketId).stream()
                .filter(item -> item.createdAt().isAfter(since))
                .collect(Collectors.toList());
    }

    // ========== MÉTODOS PRIVADOS DE COLETA ==========

    private List<TimelineItemResponse> getPublicComments(UUID ticketId) {
        return commentRepository.findPublicCommentsByTicketId(ticketId).stream()
                .map(this::commentToTimelineItem)
                .collect(Collectors.toList());
    }

    private List<TimelineItemResponse> getAllVisibleComments(UUID ticketId) {
        return commentRepository.findAllVisibleCommentsByTicketId(ticketId).stream()
                .map(this::commentToTimelineItem)
                .collect(Collectors.toList());
    }

    private List<TimelineItemResponse> getPublicEvents(UUID ticketId) {
        return eventRepository.findPublicEventsByTicketId(ticketId).stream()
                .map(this::eventToTimelineItem)
                .collect(Collectors.toList());
    }

    private List<TimelineItemResponse> getAllVisibleEvents(UUID ticketId) {
        return eventRepository.findAllVisibleEventsByTicketId(ticketId).stream()
                .map(this::eventToTimelineItem)
                .collect(Collectors.toList());
    }

    // ========== CONVERSORES ==========

    private TimelineItemResponse commentToTimelineItem(TicketComment comment) {
        TimelineItemResponse.AuthorInfo author = null;
        if (comment.getAuthor() != null) {
            author = new TimelineItemResponse.AuthorInfo(
                    comment.getAuthor().getId(),
                    comment.getAuthor().getName(),
                    comment.getAuthor().getEmail()
            );
        }

        String title = (author != null ? author.name() : "Usuário") + " adicionou um comentário";

        return new TimelineItemResponse(
                comment.getId(),
                TimelineItemResponse.TimelineItemType.COMENTARIO,
                title,
                null,
                author,
                comment.getVisibility(),
                comment.getCreatedAt(),
                comment.getContent(),
                comment.isEdited(),
                comment.isRemoved(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private TimelineItemResponse eventToTimelineItem(TicketEvent event) {
        TimelineItemResponse.AuthorInfo author = null;
        if (event.getActor() != null) {
            author = new TimelineItemResponse.AuthorInfo(
                    event.getActor().getId(),
                    event.getActor().getName(),
                    event.getActor().getEmail()
            );
        }

        return new TimelineItemResponse(
                event.getId(),
                TimelineItemResponse.TimelineItemType.EVENTO,
                event.getTitle(),
                event.getDescription(),
                author,
                event.getVisibility(),
                event.getCreatedAt(),
                null,
                null,
                null,
                event.getEventType(),
                event.getEventType().getLabel(),
                event.getOrigin(),
                event.getOldValue(),
                event.getNewValue(),
                event.getMetadata()
        );
    }

    // ========== VALIDAÇÕES ==========

    private Ticket findTicketOrThrow(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private void validateTicketAccess(Ticket ticket, User user) {
        // SOLICITANTE só vê timeline de chamados que ele abriu
        if (user.getRole() == UserRole.SOLICITANTE && !ticket.isRequestedBy(user)) {
            throw new TicketPermissionDeniedException("visualizar timeline",
                    "Você só pode ver a timeline de chamados que você abriu");
        }

        // AGENTE deve ter acesso ao chamado (ser responsável ou estar na fila)
        // TODO: Implementar regra de fila se necessário
    }
}
