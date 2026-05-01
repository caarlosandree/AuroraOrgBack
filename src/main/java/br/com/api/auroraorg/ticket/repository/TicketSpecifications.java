package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.dto.TicketFilterRequest;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifications para consultas dinâmicas de Tickets.
 * 
 * Permite construir consultas com filtros opcionais combinados com AND.
 * Usado pelo Repository para buscas complexas.
 */
public class TicketSpecifications {

    /**
     * Cria uma Specification a partir dos filtros informados.
     * Todos os filtros são opcionais e combinados com AND.
     */
    public static Specification<Ticket> withFilters(TicketFilterRequest filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por status
            if (filters.status() != null) {
                predicates.add(cb.equal(root.get("status"), filters.status()));
            }

            // Filtro por prioridade
            if (filters.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filters.priority()));
            }

            // Filtro por categoria
            if (filters.category() != null && !filters.category().isBlank()) {
                predicates.add(cb.equal(root.get("category"), filters.category()));
            }

            // Filtro por solicitante
            if (filters.requesterId() != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), filters.requesterId()));
            }

            // Filtro por responsável
            if (filters.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), filters.assigneeId()));
            }

            // Filtro por SLA vencido
            if (filters.slaOverdue() != null) {
                if (filters.slaOverdue()) {
                    predicates.add(cb.lessThan(root.get("slaDueAt"), LocalDateTime.now()));
                } else {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("slaDueAt"), LocalDateTime.now()));
                }
            }

            // Busca textual em título e descrição
            if (filters.searchTerm() != null && !filters.searchTerm().isBlank()) {
                String term = "%" + filters.searchTerm().toLowerCase() + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), term);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), term);
                predicates.add(cb.or(titlePredicate, descPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification para buscar chamados por status.
     */
    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Specification para buscar chamados por prioridade.
     */
    public static Specification<Ticket> hasPriority(TicketPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /**
     * Specification para buscar chamados de um solicitante.
     */
    public static Specification<Ticket> hasRequester(java.util.UUID requesterId) {
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), requesterId);
    }

    /**
     * Specification para buscar chamados de um responsável.
     */
    public static Specification<Ticket> hasAssignee(java.util.UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    /**
     * Specification para buscar chamados sem responsável.
     */
    public static Specification<Ticket> hasNoAssignee() {
        return (root, query, cb) -> cb.isNull(root.get("assignee"));
    }

    /**
     * Specification para buscar chamados com SLA vencido.
     */
    public static Specification<Ticket> isSlaOverdue() {
        return (root, query, cb) -> cb.lessThan(root.get("slaDueAt"), LocalDateTime.now());
    }

    /**
     * Specification para buscar chamados abertos (não finalizados).
     */
    public static Specification<Ticket> isOpen() {
        return (root, query, cb) -> root.get("status").in(
            TicketStatus.ABERTO,
            TicketStatus.EM_TRIAGEM,
            TicketStatus.EM_ATENDIMENTO,
            TicketStatus.AGUARDANDO_SOLICITANTE
        );
    }

    /**
     * Specification para busca textual.
     */
    public static Specification<Ticket> containsText(String term) {
        return (root, query, cb) -> {
            String likeTerm = "%" + term.toLowerCase() + "%";
            Predicate titlePredicate = cb.like(cb.lower(root.get("title")), likeTerm);
            Predicate descPredicate = cb.like(cb.lower(root.get("description")), likeTerm);
            return cb.or(titlePredicate, descPredicate);
        };
    }
}
