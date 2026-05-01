package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade de Eventos de Histórico de Chamados.
 *
 * Características:
 * - Imutável (não deve ser alterada após criação)
 * - Eventos são registros de auditoria permanentes
 * - Metadata em JSONB para extensibilidade
 * - Visibilidade controlada (PUBLICO/INTERNO)
 *
 * Diferença entre TicketComment e TicketEvent:
 * - Comment: conteúdo conversacional, pode ser editado/removido
 * - Event: registro de auditoria, imutável, histórico factual
 */
@Entity
@Table(name = "ticket_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
public class TicketEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Chamado ao qual o evento pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /**
     * Tipo de evento (enum).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TicketEventType eventType;

    /**
     * Usuário que causou o evento (nulo para eventos do sistema).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Origem do evento: USUARIO ou SISTEMA.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 30)
    @Builder.Default
    private TicketEventOrigin origin = TicketEventOrigin.USUARIO;

    /**
     * Título legível do evento para timeline.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Descrição detalhada do evento.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Valor anterior (para eventos de alteração).
     */
    @Column(name = "old_value", length = 500)
    private String oldValue;

    /**
     * Novo valor (para eventos de alteração).
     */
    @Column(name = "new_value", length = 500)
    private String newValue;

    /**
     * Dados adicionais flexíveis em formato JSONB.
     * Usa Hibernate 6 @JdbcTypeCode para mapeamento nativo de JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    /**
     * Visibilidade: PUBLICO (todos) ou INTERNO (equipe).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private CommentVisibility visibility = CommentVisibility.PUBLICO;

    /**
     * Data/hora de criação (imutável).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Hook executado antes de persistir.
     * Define ID e timestamp automaticamente.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Verifica se o evento é de origem do sistema.
     */
    public boolean isSystemEvent() {
        return this.origin == TicketEventOrigin.SISTEMA;
    }

    /**
     * Verifica se o evento é visível publicamente.
     */
    public boolean isPublic() {
        return this.visibility == CommentVisibility.PUBLICO;
    }

    /**
     * Verifica se o evento é interno (equipe apenas).
     */
    public boolean isInternal() {
        return this.visibility == CommentVisibility.INTERNO;
    }

    /**
     * Obtém o nome do ator ou "Sistema" se for evento automático.
     */
    public String getActorName() {
        if (isSystemEvent() || this.actor == null) {
            return "Sistema";
        }
        return this.actor.getName();
    }
}
