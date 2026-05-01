package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para operações de Eventos de Histórico de Chamados.
 *
 * Eventos são imutáveis e representam ações importantes no ciclo de vida do chamado.
 */
@Repository
public interface TicketEventRepository extends JpaRepository<TicketEvent, UUID> {

    /**
     * Busca todos os eventos de um chamado ordenados cronologicamente.
     * Uso: ADMIN com acesso total.
     */
    @EntityGraph(attributePaths = {"actor"})
    List<TicketEvent> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    /**
     * Busca eventos de um chamado com paginação.
     */
    @EntityGraph(attributePaths = {"actor"})
    Page<TicketEvent> findByTicketIdOrderByCreatedAtAsc(UUID ticketId, Pageable pageable);

    /**
     * Busca eventos públicos de um chamado.
     * Uso: SOLICITANTE visualizando próprio chamado.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.visibility = 'PUBLICO'
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findPublicEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos públicos de um chamado com paginação.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.visibility = 'PUBLICO'
        ORDER BY e.createdAt ASC
        """)
    Page<TicketEvent> findPublicEventsByTicketId(@Param("ticketId") UUID ticketId, Pageable pageable);

    /**
     * Busca todos os eventos visíveis para a equipe (públicos ou internos).
     * Uso: AGENTE, ADMIN, GESTOR.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findAllVisibleEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos visíveis para a equipe com paginação.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        ORDER BY e.createdAt ASC
        """)
    Page<TicketEvent> findAllVisibleEventsByTicketId(@Param("ticketId") UUID ticketId, Pageable pageable);

    /**
     * Busca eventos por tipo específico de um chamado.
     */
    @EntityGraph(attributePaths = {"actor"})
    List<TicketEvent> findByTicketIdAndEventTypeOrderByCreatedAtAsc(UUID ticketId, TicketEventType eventType);

    /**
     * Busca eventos de SLA de um chamado.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.eventType IN ('SLA_EM_RISCO', 'SLA_VENCIDO')
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findSlaEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos de alteração de status de um chamado.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.eventType = 'STATUS_ALTERADO'
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findStatusChangeEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos recentes de um chamado (útil para notificações).
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.createdAt > :since
        ORDER BY e.createdAt DESC
        """)
    List<TicketEvent> findRecentEventsByTicketId(
        @Param("ticketId") UUID ticketId,
        @Param("since") LocalDateTime since
    );

    /**
     * Busca eventos por ator (usuário que causou o evento).
     */
    @EntityGraph(attributePaths = {"ticket", "actor"})
    Page<TicketEvent> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    /**
     * Busca eventos de um tipo específico em um período.
     * Útil para relatórios (ex: todos os SLA vencidos no mês).
     */
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.eventType = :eventType
        AND e.createdAt BETWEEN :start AND :end
        ORDER BY e.createdAt DESC
        """)
    List<TicketEvent> findByEventTypeAndDateRange(
        @Param("eventType") TicketEventType eventType,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Verifica se existe evento de um tipo específico para o chamado.
     * Útil para evitar duplicação de eventos automáticos.
     */
    boolean existsByTicketIdAndEventType(UUID ticketId, TicketEventType eventType);

    /**
     * Conta eventos públicos de um chamado.
     */
    @Query("""
        SELECT COUNT(e) FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.visibility = 'PUBLICO'
        """)
    long countPublicEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Conta todos os eventos de um chamado.
     */
    @Query("""
        SELECT COUNT(e) FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        """)
    long countAllEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos internos de um chamado (para auditoria).
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.visibility = 'INTERNO'
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findInternalEventsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca eventos de criação de comentários de um chamado.
     */
    @EntityGraph(attributePaths = {"actor"})
    @Query("""
        SELECT e FROM TicketEvent e
        WHERE e.ticket.id = :ticketId
        AND e.eventType = 'COMENTARIO_ADICIONADO'
        ORDER BY e.createdAt ASC
        """)
    List<TicketEvent> findCommentEventsByTicketId(@Param("ticketId") UUID ticketId);
}
