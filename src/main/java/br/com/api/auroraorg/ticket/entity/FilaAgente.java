package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade de relacionamento entre Fila e Agente (N:N).
 *
 * Um agente pode participar de várias filas.
 * Uma fila pode ter vários agentes.
 */
@Entity
@Table(name = "fila_agentes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaAgente {

    @EmbeddedId
    private FilaAgenteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("filaId")
    @JoinColumn(name = "fila_id", nullable = false)
    private Fila fila;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("agenteId")
    @JoinColumn(name = "agente_id", nullable = false)
    private User agente;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = new FilaAgenteId();
        }
        this.createdAt = LocalDateTime.now();
    }
}
