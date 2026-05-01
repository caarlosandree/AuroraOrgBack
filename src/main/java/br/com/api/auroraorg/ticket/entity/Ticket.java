package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade principal de Chamado/Ticket no sistema de ITSM.
 * 
 * Mapeamento JPA com PostgreSQL:
 * - Usa UUID como chave primária
 * - Enums mapeados como STRING para segurança
 * - Relacionamentos ManyToOne com User (solicitante e responsável)
 * - Dados de auditoria automáticos via @PrePersist/@PreUpdate
 */
@Entity
@Table(name = "tickets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TicketStatus status = TicketStatus.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIA;

    @Column(name = "category", length = 100)
    private String category;

    /**
     * Solicitante do chamado (quem criou).
     * Nunca pode ser nulo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /**
     * Responsável pelo atendimento.
     * Pode ser nulo na criação até ser atribuído.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Data/hora limite do SLA baseada na prioridade.
     * Calculada automaticamente na criação.
     */
    @Column(name = "sla_due_at", nullable = false)
    private LocalDateTime slaDueAt;

    /**
     * Data/hora de resolução.
     * Preenchido apenas quando status muda para RESOLVIDO.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Hook executado antes de persistir a entidade.
     * Define: id, timestamps, status inicial.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Garante status ABERTO na criação
        if (this.status == null) {
            this.status = TicketStatus.ABERTO;
        }
        
        // Garante prioridade padrão se não informada
        if (this.priority == null) {
            this.priority = TicketPriority.MEDIA;
        }
    }

    /**
     * Hook executado antes de atualizar a entidade.
     * Atualiza o timestamp de modificação.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o chamado está atribuído a algum responsável.
     */
    public boolean hasAssignee() {
        return this.assignee != null;
    }

    /**
     * Verifica se o usuário informado é o responsável atual.
     */
    public boolean isAssignedTo(User user) {
        return this.hasAssignee() && this.assignee.getId().equals(user.getId());
    }

    /**
     * Verifica se o usuário informado é o solicitante.
     */
    public boolean isRequestedBy(User user) {
        return this.requester != null && this.requester.getId().equals(user.getId());
    }

    /**
     * Marca o chamado como resolvido, preenchendo a data de resolução.
     */
    public void markAsResolved() {
        this.status = TicketStatus.RESOLVIDO;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Reabre um chamado resolvido, limpando a data de resolução.
     */
    public void reopen() {
        this.status = TicketStatus.EM_ATENDIMENTO;
        this.resolvedAt = null;
    }

    /**
     * Cancela o chamado.
     */
    public void cancel() {
        this.status = TicketStatus.CANCELADO;
    }

    /**
     * Fecha o chamado resolvido.
     */
    public void close() {
        this.status = TicketStatus.FECHADO;
    }

    /**
     * Verifica se o SLA está vencido.
     */
    public boolean isSlaOverdue() {
        return LocalDateTime.now().isAfter(this.slaDueAt);
    }

    /**
     * Calcula o tempo restante do SLA em horas.
     */
    public long getRemainingSlaHours() {
        if (isSlaOverdue()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), this.slaDueAt).toHours();
    }
}
