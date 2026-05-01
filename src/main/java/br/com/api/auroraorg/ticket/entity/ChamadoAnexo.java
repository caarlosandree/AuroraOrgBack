package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de Anexo de Chamado.
 *
 * Armazena apenas metadados do arquivo — nunca o conteúdo binário.
 * O arquivo físico é gerenciado pelo ArquivoStorageService.
 *
 * Soft delete: o campo {@code removed} indica remoção lógica.
 * O arquivo físico é preservado para fins de auditoria.
 */
@Entity
@Table(name = "ticket_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChamadoAnexo {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Chamado ao qual o anexo pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    /**
     * Nome gerado para armazenamento seguro (UUID + extensão).
     * Nunca confiar no nome original para salvar em disco.
     */
    @Column(name = "file_name", nullable = false, length = 255, updatable = false)
    private String fileName;

    /**
     * Nome original enviado pelo usuário — usado apenas para exibição e download.
     */
    @Column(name = "original_file_name", nullable = false, length = 255, updatable = false)
    private String originalFileName;

    /**
     * MIME type declarado pelo cliente e validado pelo servidor.
     */
    @Column(name = "content_type", nullable = false, length = 127, updatable = false)
    private String contentType;

    /**
     * Tamanho do arquivo em bytes.
     */
    @Column(name = "file_size", nullable = false, updatable = false)
    private long fileSize;

    /**
     * Caminho relativo no storage — nunca expor diretamente na API pública.
     * Exemplo: chamados/{ticketId}/anexos/{fileName}
     */
    @Column(name = "storage_path", nullable = false, length = 512, updatable = false)
    private String storagePath;

    /**
     * Usuário que realizou o upload.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Soft delete — quando TRUE o anexo não deve aparecer para usuários comuns.
     */
    @Column(name = "removed", nullable = false)
    @Builder.Default
    private boolean removed = false;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    /**
     * Usuário que realizou a remoção lógica.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by")
    private User removedBy;

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
     * Aplica a remoção lógica do anexo.
     */
    public void removerLogicamente(User actor) {
        this.removed = true;
        this.removedAt = LocalDateTime.now();
        this.removedBy = actor;
    }

    /**
     * Indica se o arquivo ainda está disponível para download.
     */
    public boolean estaDisponivel() {
        return !this.removed;
    }
}
