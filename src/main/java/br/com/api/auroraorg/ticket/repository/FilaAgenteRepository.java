package br.com.api.auroraorg.ticket.repository;

import br.com.api.auroraorg.ticket.entity.FilaAgente;
import br.com.api.auroraorg.ticket.entity.FilaAgenteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FilaAgenteRepository extends JpaRepository<FilaAgente, FilaAgenteId> {

    List<FilaAgente> findAllByFilaId(UUID filaId);

    @Query("SELECT fa FROM FilaAgente fa JOIN FETCH fa.agente WHERE fa.fila.id = :filaId")
    List<FilaAgente> findAllByFilaIdWithAgente(UUID filaId);

    boolean existsByFilaIdAndAgenteId(UUID filaId, UUID agenteId);

    @Query("SELECT fa.fila.id FROM FilaAgente fa WHERE fa.agente.id = :agenteId")
    List<UUID> findFilaIdsByAgenteId(UUID agenteId);

    @Query("SELECT COUNT(fa) > 0 FROM FilaAgente fa WHERE fa.fila.id = :filaId AND fa.agente.id = :agenteId")
    boolean isAgenteInFila(UUID filaId, UUID agenteId);
}
