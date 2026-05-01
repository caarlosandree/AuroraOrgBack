package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.entity.TicketEvent;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketEventOrigin;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.ticket.repository.TicketEventRepository;
import br.com.api.auroraorg.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para TicketHistoryService.
 *
 * Cobertura:
 * - Registrar evento de criação de chamado
 * - Registrar evento de alteração de status
 * - Registrar evento de atribuição de responsável
 * - Registrar evento de resolução
 * - Registrar evento de SLA em risco
 * - Verificar imutabilidade dos eventos
 */
@ExtendWith(MockitoExtension.class)
class TicketHistoryServiceTest {

    @Mock
    private TicketEventRepository eventRepository;

    @InjectMocks
    private TicketHistoryService historyService;

    private User admin;
    private User agent;
    private User requester;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        admin = createUser(UUID.randomUUID(), "admin@test.com", "Admin", UserRole.ADMIN);
        agent = createUser(UUID.randomUUID(), "agente@test.com", "Agente", UserRole.AGENTE);
        requester = createUser(UUID.randomUUID(), "solicitante@test.com", "Solicitante", UserRole.SOLICITANTE);

        ticket = createTicket(UUID.randomUUID(), "Chamado Teste", requester, null, TicketStatus.ABERTO);
    }

    @Test
    @DisplayName("Deve registrar evento de criação de chamado")
    void shouldRecordTicketCreatedEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordTicketCreated(ticket, requester);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.CHAMADO_CRIADO);
        assertThat(result.getActor()).isEqualTo(requester);
        assertThat(result.getOrigin()).isEqualTo(TicketEventOrigin.USUARIO);
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
        assertThat(result.getTitle()).contains("abriu o chamado");

        verify(eventRepository).save(any(TicketEvent.class));
    }

    @Test
    @DisplayName("Deve registrar evento de alteração de status")
    void shouldRecordStatusChangedEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordStatusChanged(ticket, agent, TicketStatus.ABERTO, TicketStatus.EM_ATENDIMENTO);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.STATUS_ALTERADO);
        assertThat(result.getOldValue()).isEqualTo("ABERTO");
        assertThat(result.getNewValue()).isEqualTo("EM_ATENDIMENTO");
        assertThat(result.getTitle()).contains("alterou o status");
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
    }

    @Test
    @DisplayName("Deve registrar evento de atribuição de responsável")
    void shouldRecordAssigneeAssignedEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordAssigneeAssigned(ticket, admin, agent);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.RESPONSAVEL_ATRIBUIDO);
        assertThat(result.getNewValue()).isEqualTo(agent.getName());
        assertThat(result.getTitle()).contains("atribuiu");
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
    }

    @Test
    @DisplayName("Deve registrar evento de troca de responsável como INTERNO")
    void shouldRecordAssigneeChangedAsInternal() {
        // Arrange
        User oldAgent = createUser(UUID.randomUUID(), "velho@test.com", "Agente Antigo", UserRole.AGENTE);
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordAssigneeChanged(ticket, admin, oldAgent, agent);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.RESPONSAVEL_ALTERADO);
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.INTERNO);
        assertThat(result.getOldValue()).isEqualTo(oldAgent.getName());
        assertThat(result.getNewValue()).isEqualTo(agent.getName());
    }

    @Test
    @DisplayName("Deve registrar evento de resolução de chamado")
    void shouldRecordTicketResolvedEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordTicketResolved(ticket, agent);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.CHAMADO_RESOLVIDO);
        assertThat(result.getTitle()).contains("resolveu");
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
    }

    @Test
    @DisplayName("Deve registrar evento de fechamento de chamado")
    void shouldRecordTicketClosedEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordTicketClosed(ticket, requester);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.CHAMADO_FECHADO);
        assertThat(result.getTitle()).contains("fechou");
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
    }

    @Test
    @DisplayName("Deve registrar evento de cancelamento de chamado")
    void shouldRecordTicketCancelledEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordTicketCancelled(ticket, requester);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.CHAMADO_CANCELADO);
        assertThat(result.getTitle()).contains("cancelou");
    }

    @Test
    @DisplayName("Deve registrar evento de SLA em risco como origem SISTEMA")
    void shouldRecordSlaAtRiskAsSystemEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordSlaAtRisk(ticket);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.SLA_EM_RISCO);
        assertThat(result.getOrigin()).isEqualTo(TicketEventOrigin.SISTEMA);
        assertThat(result.getActor()).isNull();
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.INTERNO);
        assertThat(result.getTitle()).contains("Sistema");
    }

    @Test
    @DisplayName("Deve registrar evento de SLA vencido como origem SISTEMA")
    void shouldRecordSlaOverdueAsSystemEvent() {
        // Arrange
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // Act
        TicketEvent result = historyService.recordSlaOverdue(ticket);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.SLA_VENCIDO);
        assertThat(result.getOrigin()).isEqualTo(TicketEventOrigin.SISTEMA);
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.INTERNO);
    }

    @Test
    @DisplayName("Deve registrar evento de adição de comentário com visibilidade do comentário")
    void shouldRecordCommentAddedWithCommentVisibility() {
        // Arrange
        TicketComment publicComment = createComment(CommentVisibility.PUBLICO);
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TicketEvent result = historyService.recordCommentAdded(ticket, agent, publicComment);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.COMENTARIO_ADICIONADO);
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.PUBLICO);
    }

    @Test
    @DisplayName("Deve registrar evento de remoção de comentário como INTERNO")
    void shouldRecordCommentRemovedAsInternal() {
        // Arrange
        TicketComment comment = createComment(CommentVisibility.PUBLICO);
        when(eventRepository.save(any(TicketEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TicketEvent result = historyService.recordCommentRemoved(ticket, admin, comment);

        // Assert
        assertThat(result.getEventType()).isEqualTo(TicketEventType.COMENTARIO_REMOVIDO);
        assertThat(result.getVisibility()).isEqualTo(CommentVisibility.INTERNO);
    }

    @Test
    @DisplayName("Deve armazenar metadata corretamente no evento")
    void shouldStoreMetadataInEvent() {
        // Arrange
        ArgumentCaptor<TicketEvent> eventCaptor = ArgumentCaptor.forClass(TicketEvent.class);
        when(eventRepository.save(eventCaptor.capture())).thenAnswer(invocation -> {
            TicketEvent event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        // Act
        historyService.recordStatusChanged(ticket, agent, TicketStatus.ABERTO, TicketStatus.EM_ATENDIMENTO);

        // Assert
        TicketEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getMetadata()).isNotNull();
        assertThat(savedEvent.getMetadata()).containsKey("oldStatusLabel");
        assertThat(savedEvent.getMetadata()).containsKey("newStatusLabel");
    }

    // ========== MÉTODOS AUXILIARES ==========

    private User createUser(UUID id, String email, String name, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Ticket createTicket(UUID id, String title, User requester, User assignee, TicketStatus status) {
        return Ticket.builder()
                .id(id)
                .title(title)
                .description("Descrição do chamado")
                .status(status)
                .priority(TicketPriority.MEDIA)
                .requester(requester)
                .assignee(assignee)
                .slaDueAt(LocalDateTime.now().plusHours(48))
                .build();
    }

    private TicketComment createComment(CommentVisibility visibility) {
        TicketComment comment = new TicketComment();
        comment.setId(UUID.randomUUID());
        comment.setTicket(ticket);
        comment.setAuthor(agent);
        comment.setContent("Conteúdo do comentário");
        comment.setVisibility(visibility);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setEdited(false);
        comment.setRemoved(false);
        return comment;
    }
}
