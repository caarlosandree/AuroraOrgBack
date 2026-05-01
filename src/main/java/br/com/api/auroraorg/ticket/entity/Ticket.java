package br.com.api.auroraorg.ticket.entity;

import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
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
     * Fila de atendimento atual do chamado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fila_id")
    private Fila fila;

    /**
     * Categoria de classificação do chamado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

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
     * Data/hora limite para primeira resposta do agente.
     */
    @Column(name = "prazo_primeira_resposta")
    private LocalDateTime prazoPrimeiraResposta;

    /**
     * Data/hora limite para resolução do chamado.
     * Substitui sla_due_at com granularidade maior.
     */
    @Column(name = "prazo_resolucao")
    private LocalDateTime prazoResolucao;

    /**
     * Data/hora em que o agente respondeu pela primeira vez.
     */
    @Column(name = "data_primeira_resposta")
    private LocalDateTime dataPrimeiraResposta;

    /**
     * Data/hora em que o chamado foi resolvido (pode coincidir com resolved_at).
     */
    @Column(name = "data_resolucao")
    private LocalDateTime dataResolucao;

    /**
     * Status do SLA de primeira resposta.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_primeira_resposta_status", length = 30)
    @Builder.Default
    private SlaStatus slaPrimeiraRespostaStatus = SlaStatus.DENTRO_DO_PRAZO;

    /**
     * Status do SLA de resolução.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_resolucao_status", length = 30)
    @Builder.Default
    private SlaStatus slaResolucaoStatus = SlaStatus.DENTRO_DO_PRAZO;

    /**
     * Usuário que deu a primeira resposta ao chamado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primeira_resposta_por")
    private User primeiraRespostaPor;

    /**
     * Tempo decorrido em segundos até a primeira resposta.
     */
    @Column(name = "tempo_primeira_resposta_segundos")
    private Long tempoPrimeiraRespostaSegundos;

    /**
     * Tempo decorrido em segundos até a resolução.
     */
    @Column(name = "tempo_resolucao_segundos")
    private Long tempoResolucaoSegundos;

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
     * Verifica se o SLA de resolução está vencido (retrocompatibilidade).
     */
    public boolean isSlaOverdue() {
        if (this.prazoResolucao != null) {
            return LocalDateTime.now().isAfter(this.prazoResolucao);
        }
        return LocalDateTime.now().isAfter(this.slaDueAt);
    }

    /**
     * Calcula o tempo restante do SLA de resolução em horas (retrocompatibilidade).
     */
    public long getRemainingSlaHours() {
        LocalDateTime prazo = this.prazoResolucao != null ? this.prazoResolucao : this.slaDueAt;
        if (prazo == null) {
            return 0;
        }
        if (LocalDateTime.now().isAfter(prazo)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), prazo).toHours();
    }

    /**
     * Verifica se o chamado permite atualização de SLA.
     * Chamados RESOLVIDOS, FECHADOS ou CANCELADOS não permitem.
     */
    public boolean allowsSlaUpdate() {
        return this.status != TicketStatus.RESOLVIDO && !this.status.isTerminal();
    }

    /**
     * Verifica se já houve primeira resposta registrada.
     */
    public boolean hasPrimeiraResposta() {
        return this.dataPrimeiraResposta != null;
    }

    /**
     * Registra a primeira resposta ao chamado.
     */
    public void registrarPrimeiraResposta(User agente) {
        this.dataPrimeiraResposta = LocalDateTime.now();
        this.primeiraRespostaPor = agente;
        this.tempoPrimeiraRespostaSegundos = Duration.between(this.createdAt, this.dataPrimeiraResposta).getSeconds();
    }

    /**
     * Registra a resolução do chamado.
     */
    public void registrarResolucao() {
        this.dataResolucao = LocalDateTime.now();
        this.tempoResolucaoSegundos = Duration.between(this.createdAt, this.dataResolucao).getSeconds();
    }

    /**
     * Verifica se o chamado está em status que permite monitoramento de SLA.
     */
    public boolean isSlaMonitored() {
        return this.status == TicketStatus.ABERTO
                || this.status == TicketStatus.EM_TRIAGEM
                || this.status == TicketStatus.EM_ATENDIMENTO
                || this.status == TicketStatus.AGUARDANDO_SOLICITANTE;
    }
}
