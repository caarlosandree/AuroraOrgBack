package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.ChamadoAnexo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para operações de Anexos de Chamados.
 */
@Repository
public interface ChamadoAnexoRepository extends JpaRepository<ChamadoAnexo, UUID> {

    /**
     * Lista anexos ativos (não removidos) de um chamado.
     */
    @EntityGraph(attributePaths = {"uploadedBy"})
    @Query("""
        SELECT a FROM ChamadoAnexo a
        WHERE a.ticket.id = :ticketId
          AND a.removed = false
        ORDER BY a.createdAt ASC
        """)
    List<ChamadoAnexo> findActiveByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Busca um anexo ativo por ID e chamado (evita acesso cruzado entre chamados).
     */
    @EntityGraph(attributePaths = {"uploadedBy", "ticket"})
    @Query("""
        SELECT a FROM ChamadoAnexo a
        WHERE a.id = :anexoId
          AND a.ticket.id = :ticketId
          AND a.removed = false
        """)
    Optional<ChamadoAnexo> findActiveByIdAndTicketId(
            @Param("anexoId") UUID anexoId,
            @Param("ticketId") UUID ticketId);

    /**
     * Busca qualquer anexo por ID e chamado (incluindo removidos — uso admin/auditoria).
     */
    @EntityGraph(attributePaths = {"uploadedBy", "removedBy", "ticket"})
    Optional<ChamadoAnexo> findByIdAndTicketId(UUID id, UUID ticketId);

    /**
     * Conta anexos ativos de um chamado — usado para impor o limite máximo.
     */
    @Query("""
        SELECT COUNT(a) FROM ChamadoAnexo a
        WHERE a.ticket.id = :ticketId
          AND a.removed = false
        """)
    long countActiveByTicketId(@Param("ticketId") UUID ticketId);

    /**
     * Lista todos os anexos de um chamado enviados por um usuário específico.
     */
    @EntityGraph(attributePaths = {"uploadedBy"})
    @Query("""
        SELECT a FROM ChamadoAnexo a
        WHERE a.ticket.id = :ticketId
          AND a.uploadedBy.id = :userId
          AND a.removed = false
        ORDER BY a.createdAt ASC
        """)
    List<ChamadoAnexo> findActiveByTicketIdAndUploadedBy(
            @Param("ticketId") UUID ticketId,
            @Param("userId") UUID userId);
}
