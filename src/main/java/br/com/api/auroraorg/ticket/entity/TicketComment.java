package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de Comentários em Chamados.
 *
 * Características:
 * - Soft delete (campo `removed`)
 * - Controle de edição (campo `edited`)
 * - Visibilidade (PUBLICO ou INTERNO)
 * - Relacionamento com Ticket e User
 *
 * Regras de negócio:
 * - Comentários não são excluídos fisicamente
 * - Apenas autor ou ADMIN pode editar/remover
 * - SOLICITANTE só pode criar comentários PUBLICO
 * - INTERNO nunca é visível para SOLICITANTE
 */
@Entity
@Table(name = "ticket_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketComment {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Chamado ao qual o comentário pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /**
     * Autor do comentário.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Conteúdo textual do comentário (máx 5000 caracteres).
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Visibilidade: PUBLICO (todos) ou INTERNO (equipe).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private CommentVisibility visibility = CommentVisibility.PUBLICO;

    /**
     * Data/hora de criação.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data/hora da última atualização.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Indica se o comentário foi editado.
     */
    @Column(name = "edited", nullable = false)
    @Builder.Default
    private Boolean edited = false;

    /**
     * Soft delete - indica se o comentário foi removido logicamente.
     */
    @Column(name = "removed", nullable = false)
    @Builder.Default
    private Boolean removed = false;

    /**
     * Hook executado antes de persistir.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.edited == null) {
            this.edited = false;
        }
        if (this.removed == null) {
            this.removed = false;
        }
    }

    /**
     * Hook executado antes de atualizar.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o comentário é visível publicamente.
     */
    public boolean isPublic() {
        return this.visibility == CommentVisibility.PUBLICO && !this.removed;
    }

    /**
     * Verifica se o comentário foi removido.
     */
    public boolean isRemoved() {
        return this.removed;
    }

    /**
     * Verifica se o comentário foi editado.
     */
    public boolean isEdited() {
        return this.edited;
    }

    /**
     * Marca o comentário como removido (soft delete).
     */
    public void markAsRemoved() {
        this.removed = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Atualiza o conteúdo e marca como editado.
     */
    public void updateContent(String newContent) {
        this.content = newContent;
        this.edited = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o usuário informado é o autor.
     */
    public boolean isAuthor(User user) {
        return this.author != null && this.author.getId().equals(user.getId());
    }
}
