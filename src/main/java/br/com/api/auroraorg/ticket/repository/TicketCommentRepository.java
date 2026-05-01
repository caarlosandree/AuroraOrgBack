package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de Comentários de Chamados.
 *
 * Oferece métodos para consulta com filtros de visibilidade e paginação.
 */
@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    /**
     * Busca todos os comentários de um chamado (incluindo removidos).
     * Uso: ADMIN com acesso total.
     */
    @EntityGraph(attributePaths = {"author"})
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    /**
     * Busca todos os comentários de um chamado com paginação.
     */
    @EntityGraph(attributePaths = {"author"})
    Page<TicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId, Pageable pageable);

    /**
     * Busca comentários visíveis para o público (não removidos e públicos).
     * Uso: SOLICITANTE visualizando próprio chamado.
     */
    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT c FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.removed = false
        AND c.visibility = 'PUBLICO'
        ORDER BY c.createdAt ASC
        """)
    List<TicketComment> findPublicCommentsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca comentários visíveis para a equipe (não removidos, públicos ou internos).
     * Uso: AGENTE, ADMIN, GESTOR.
     */
    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT c FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.removed = false
        ORDER BY c.createdAt ASC
        """)
    List<TicketComment> findAllVisibleCommentsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca comentários visíveis para a equipe com paginação.
     */
    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT c FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.removed = false
        ORDER BY c.createdAt ASC
        """)
    Page<TicketComment> findAllVisibleCommentsByTicketId(@Param("ticketId") UUID ticketId, Pageable pageable);

    /**
     * Busca comentário por ID garantindo que pertence ao chamado especificado.
     */
    @EntityGraph(attributePaths = {"author", "ticket"})
    Optional<TicketComment> findByIdAndTicketId(UUID id, UUID ticketId);

    /**
     * Conta comentários públicos não removidos de um chamado.
     */
    @Query("""
        SELECT COUNT(c) FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.removed = false
        AND c.visibility = 'PUBLICO'
        """)
    long countPublicCommentsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Conta todos os comentários não removidos de um chamado.
     */
    @Query("""
        SELECT COUNT(c) FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.removed = false
        """)
    long countAllCommentsByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca comentários por autor (útil para "meus comentários").
     */
    @EntityGraph(attributePaths = {"ticket", "author"})
    Page<TicketComment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /**
     * Verifica se existe comentário não removido do autor no chamado.
     */
    boolean existsByTicketIdAndAuthorIdAndRemovedFalse(UUID ticketId, UUID authorId);

    /**
     * Busca comentários internos de um chamado (para auditoria).
     */
    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT c FROM TicketComment c
        WHERE c.ticket.id = :ticketId
        AND c.visibility = 'INTERNO'
        ORDER BY c.createdAt ASC
        """)
    List<TicketComment> findInternalCommentsByTicketId(@Param("ticketId") UUID ticketId);
}
