package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de Política Configurável de SLA.
 *
 * Preparada para evolução futura:
 * - SLA customizado por fila
 * - SLA customizado por categoria
 * - SLA customizado por cliente (multi-tenant)
 * - Horário comercial e feriados (futuro)
 *
 * No MVP, apenas políticas padrão por prioridade são usadas.
 */
@Entity
@Table(name = "sla_politicas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaPolitica {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 30)
    private TicketPriority prioridade;

    @Column(name = "prazo_primeira_resposta_minutos", nullable = false)
    private Integer prazoPrimeiraRespostaMinutos;

    @Column(name = "prazo_resolucao_minutos", nullable = false)
    private Integer prazoResolucaoMinutos;

    @Column(name = "ativa", nullable = false)
    @Builder.Default
    private Boolean ativa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fila_id")
    private Fila fila;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.ativa == null) {
            this.ativa = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Retorna a duração do prazo de primeira resposta em minutos.
     */
    public long getPrazoPrimeiraRespostaEmMinutos() {
        return prazoPrimeiraRespostaMinutos != null ? prazoPrimeiraRespostaMinutos.longValue() : 0;
    }

    /**
     * Retorna a duração do prazo de resolução em minutos.
     */
    public long getPrazoResolucaoEmMinutos() {
        return prazoResolucaoMinutos != null ? prazoResolucaoMinutos.longValue() : 0;
    }
}
