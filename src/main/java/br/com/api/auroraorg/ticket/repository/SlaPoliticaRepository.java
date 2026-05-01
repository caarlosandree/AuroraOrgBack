package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.SlaPolitica;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para políticas configuráveis de SLA.
 *
 * No MVP, busca políticas padrão por prioridade.
 * Futuro: busca por fila, categoria e cliente.
 */
@Repository
public interface SlaPoliticaRepository extends JpaRepository<SlaPolitica, UUID> {

    /**
     * Busca política ativa por prioridade (padrão do sistema).
     */
    Optional<SlaPolitica> findByAtivaTrueAndPrioridadeAndFilaIsNullAndCategoriaIsNull(TicketPriority prioridade);

    /**
     * Busca política ativa por prioridade e fila.
     */
    Optional<SlaPolitica> findByAtivaTrueAndPrioridadeAndFilaId(TicketPriority prioridade, UUID filaId);

    /**
     * Busca política ativa por prioridade e categoria.
     */
    Optional<SlaPolitica> findByAtivaTrueAndPrioridadeAndCategoriaId(TicketPriority prioridade, UUID categoriaId);

    /**
     * Busca política ativa por prioridade, fila e categoria.
     */
    Optional<SlaPolitica> findByAtivaTrueAndPrioridadeAndFilaIdAndCategoriaId(
            TicketPriority prioridade, UUID filaId, UUID categoriaId);

    /**
     * Busca política mais específica possível para o contexto do chamado.
     * Ordem de especificidade: (prioridade + fila + categoria) > (prioridade + fila) > (prioridade + categoria) > (prioridade).
     */
    @Query("""
        SELECT sp FROM SlaPolitica sp
        WHERE sp.ativa = true
        AND sp.prioridade = :prioridade
        AND (sp.fila.id = :filaId OR sp.fila IS NULL)
        AND (sp.categoria.id = :categoriaId OR sp.categoria IS NULL)
        ORDER BY
            CASE WHEN sp.fila IS NOT NULL AND sp.categoria IS NOT NULL THEN 0 ELSE 1 END,
            CASE WHEN sp.fila IS NOT NULL THEN 0 ELSE 1 END,
            CASE WHEN sp.categoria IS NOT NULL THEN 0 ELSE 1 END
        LIMIT 1
        """)
    Optional<SlaPolitica> findMostSpecificActivePolicy(
            @Param("prioridade") TicketPriority prioridade,
            @Param("filaId") UUID filaId,
            @Param("categoriaId") UUID categoriaId);

    List<SlaPolitica> findByAtivaTrueOrderByPrioridadeAsc();

    List<SlaPolitica> findAllByOrderByCreatedAtDesc();
}
