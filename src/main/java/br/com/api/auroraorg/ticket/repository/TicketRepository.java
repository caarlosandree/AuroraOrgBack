package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de banco de dados de Tickets.
 * 
 * Extende JpaRepository para operações CRUD e JpaSpecificationExecutor
 * para consultas dinâmicas com filtros complexos.
 * 
 * Conforme backend-data.mdc:
 * - Usa EntityGraph para evitar N+1 em relacionamentos
 * - Queries paginadas para grandes volumes
 * - Specifications para filtros dinâmicos
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    // ========== BUSCAS POR STATUS ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByStatus(TicketStatus status);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    // ========== BUSCAS POR PRIORIDADE ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByPriority(TicketPriority priority);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findByPriority(TicketPriority priority, Pageable pageable);

    // ========== BUSCAS POR SOLICITANTE ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByRequesterId(UUID requesterId);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findByRequesterId(UUID requesterId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByRequesterIdAndStatus(UUID requesterId, TicketStatus status);

    // ========== BUSCAS POR RESPONSÁVEL ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByAssigneeId(UUID assigneeId);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findByAssigneeId(UUID assigneeId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findByAssigneeIdAndStatus(UUID assigneeId, TicketStatus status);

    // ========== CONSULTAS ESPECIALIZADAS ==========
    
    /**
     * Busca chamados abertos (não finalizados).
     * Status considerados abertos: ABERTO, EM_TRIAGEM, EM_ATENDIMENTO, AGUARDANDO_SOLICITANTE
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.status IN (
            br.com.api.auroraorg.ticket.enums.TicketStatus.ABERTO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.EM_TRIAGEM,
            br.com.api.auroraorg.ticket.enums.TicketStatus.EM_ATENDIMENTO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.AGUARDANDO_SOLICITANTE
        )
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findOpenTickets();
    
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.status IN (
            br.com.api.auroraorg.ticket.enums.TicketStatus.ABERTO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.EM_TRIAGEM,
            br.com.api.auroraorg.ticket.enums.TicketStatus.EM_ATENDIMENTO,
            br.com.auroraorg.ticket.enums.TicketStatus.AGUARDANDO_SOLICITANTE
        )
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findOpenTickets(Pageable pageable);

    /**
     * Busca chamados sem responsável atribuído.
     */
    @EntityGraph(attributePaths = {"requester"})
    List<Ticket> findByAssigneeIsNullAndStatusNotIn(
        List<TicketStatus> excludedStatuses
    );
    
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.assignee IS NULL
        AND t.status NOT IN (
            br.com.api.auroraorg.ticket.enums.TicketStatus.RESOLVIDO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.FECHADO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.CANCELADO
        )
        ORDER BY t.priority DESC, t.createdAt ASC
        """)
    @EntityGraph(attributePaths = {"requester"})
    List<Ticket> findUnassignedTickets();

    // ========== CONSULTAS COM FILTROS COMPLEXOS ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    // ========== CONSULTAS DE SLA ==========
    
    /**
     * Busca chamados com SLA vencido.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.slaDueAt < :now
        AND t.status NOT IN (
            br.com.api.auroraorg.ticket.enums.TicketStatus.RESOLVIDO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.FECHADO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.CANCELADO
        )
        ORDER BY t.slaDueAt ASC
        """)
    @EntityGraph(attributePaths = {"requester", "assignee"})
    List<Ticket> findOverdueTickets(@Param("now") LocalDateTime now);
    
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.slaDueAt < :now
        AND t.status NOT IN (
            br.com.api.auroraorg.ticket.enums.TicketStatus.RESOLVIDO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.FECHADO,
            br.com.api.auroraorg.ticket.enums.TicketStatus.CANCELADO
        )
        """)
    long countOverdueTickets(@Param("now") LocalDateTime now);

    // ========== BUSCA COM BUSCA TEXTO ==========
    
    /**
     * Busca chamados por termo no título ou descrição.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE (
            LOWER(t.title) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :term, '%'))
        )
        ORDER BY t.createdAt DESC
        """)
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> searchByTerm(@Param("term") String term, Pageable pageable);

    // ========== ESTATÍSTICAS ==========
    
    @Query("""
        SELECT t.status, COUNT(t) 
        FROM Ticket t 
        GROUP BY t.status
        """)
    List<Object[]> countByStatus();
    
    @Query("""
        SELECT t.priority, COUNT(t) 
        FROM Ticket t 
        GROUP BY t.priority
        """)
    List<Object[]> countByPriority();

    // ========== BUSCA COM ID ==========
    
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Optional<Ticket> findById(UUID id);
}
