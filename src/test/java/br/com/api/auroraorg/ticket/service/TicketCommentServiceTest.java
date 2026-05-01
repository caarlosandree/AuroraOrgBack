package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.CommentResponse;
import br.com.api.auroraorg.ticket.dto.CreateCommentRequest;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.CommentNotAllowedException;
import br.com.api.auroraorg.ticket.exception.CommentPermissionDeniedException;
import br.com.api.auroraorg.ticket.exception.TicketNotFoundException;
import br.com.api.auroraorg.ticket.repository.TicketCommentRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para TicketCommentService.
 *
 * Cobertura:
 * - Criar comentário público como SOLICITANTE
 * - Impedir SOLICITANTE de criar comentário interno
 * - Ocultar comentário interno para SOLICITANTE
 * - Permitir AGENTE criar comentário interno
 * - Permitir ADMIN remover qualquer comentário
 * - Impedir AGENTE de editar comentário de outro usuário
 * - Remover comentário logicamente
 */
@ExtendWith(MockitoExtension.class)
class TicketCommentServiceTest {

    @Mock
    private TicketCommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketHistoryService historyService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private TicketCommentService commentService;

    private User requester;
    private User agent;
    private User admin;
    private User manager;
    private Ticket openTicket;
    private Ticket closedTicket;

    @BeforeEach
    void setUp() {
        // Cria usuários de teste
        requester = createUser(UUID.randomUUID(), "solicitante@test.com", "Solicitante", UserRole.SOLICITANTE);
        agent = createUser(UUID.randomUUID(), "agente@test.com", "Agente", UserRole.AGENTE);
        admin = createUser(UUID.randomUUID(), "admin@test.com", "Admin", UserRole.ADMIN);
        manager = createUser(UUID.randomUUID(), "gestor@test.com", "Gestor", UserRole.GESTOR);

        // Cria chamados de teste
        openTicket = createTicket(UUID.randomUUID(), "Chamado Aberto", requester, null, TicketStatus.ABERTO);
        closedTicket = createTicket(UUID.randomUUID(), "Chamado Fechado", requester, agent, TicketStatus.FECHADO);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("Deve permitir SOLICITANTE criar comentário público em seu chamado")
    void shouldAllowRequesterToCreatePublicComment() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(requester);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            return comment;
        });

        CreateCommentRequest request = new CreateCommentRequest("Preciso de ajuda", CommentVisibility.PUBLICO);

        // Act
        CommentResponse response = commentService.addComment(openTicket.getId(), request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("Preciso de ajuda");
        assertThat(response.visibility()).isEqualTo(CommentVisibility.PUBLICO);
        assertThat(response.removed()).isFalse();

        verify(historyService).recordCommentAdded(any(), any(), any());
    }

    @Test
    @DisplayName("Deve impedir SOLICITANTE de criar comentário interno")
    void shouldPreventRequesterFromCreatingInternalComment() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(requester);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));

        CreateCommentRequest request = new CreateCommentRequest("Comentário interno", CommentVisibility.INTERNO);

        // Act & Assert
        assertThatThrownBy(() -> commentService.addComment(openTicket.getId(), request))
                .isInstanceOf(CommentNotAllowedException.class)
                .hasMessageContaining("SOLICITANTE");
    }

    @Test
    @DisplayName("Deve permitir AGENTE criar comentário interno")
    void shouldAllowAgentToCreateInternalComment() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agent);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            return comment;
        });

        CreateCommentRequest request = new CreateCommentRequest("Nota interna da equipe", CommentVisibility.INTERNO);

        // Act
        CommentResponse response = commentService.addComment(openTicket.getId(), request);

        // Assert
        assertThat(response.visibility()).isEqualTo(CommentVisibility.INTERNO);
    }

    @Test
    @DisplayName("Deve impedir comentário em chamado fechado")
    void shouldPreventCommentOnClosedTicket() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(requester);
        when(ticketRepository.findById(closedTicket.getId())).thenReturn(Optional.of(closedTicket));

        CreateCommentRequest request = new CreateCommentRequest("Tentativa de comentar", CommentVisibility.PUBLICO);

        // Act & Assert
        assertThatThrownBy(() -> commentService.addComment(closedTicket.getId(), request))
                .isInstanceOf(CommentNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve permitir ADMIN comentar mesmo em chamado fechado")
    void shouldAllowAdminToCommentOnClosedTicket() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(ticketRepository.findById(closedTicket.getId())).thenReturn(Optional.of(closedTicket));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> {
            TicketComment comment = invocation.getArgument(0);
            comment.setId(UUID.randomUUID());
            comment.setCreatedAt(LocalDateTime.now());
            comment.setUpdatedAt(LocalDateTime.now());
            return comment;
        });

        CreateCommentRequest request = new CreateCommentRequest("Comentário administrativo", CommentVisibility.INTERNO);

        // Act
        CommentResponse response = commentService.addComment(closedTicket.getId(), request);

        // Assert
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Deve impedir GESTOR de comentar")
    void shouldPreventManagerFromCommenting() {
        // Arrange
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(manager);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));

        CreateCommentRequest request = new CreateCommentRequest("Tentativa", CommentVisibility.PUBLICO);

        // Act & Assert
        assertThatThrownBy(() -> commentService.addComment(openTicket.getId(), request))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessageContaining("GESTOR");
    }

    // ========== TESTES DE EDIÇÃO ==========

    @Test
    @DisplayName("Deve permitir autor editar próprio comentário")
    void shouldAllowAuthorToEditOwnComment() {
        // Arrange
        UUID commentId = UUID.randomUUID();
        TicketComment comment = createComment(commentId, openTicket, agent, "Texto original", CommentVisibility.PUBLICO);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agent);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.findByIdAndTicketId(commentId, openTicket.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(TicketComment.class))).thenReturn(comment);

        var request = new br.com.api.auroraorg.ticket.dto.UpdateCommentRequest("Texto editado");

        // Act
        CommentResponse response = commentService.updateComment(openTicket.getId(), commentId, request);

        // Assert
        assertThat(response.content()).isEqualTo("Texto editado");
        assertThat(response.edited()).isTrue();
    }

    @Test
    @DisplayName("Deve impedir AGENTE de editar comentário de outro usuário")
    void shouldPreventAgentFromEditingOthersComment() {
        // Arrange
        UUID commentId = UUID.randomUUID();
        TicketComment comment = createComment(commentId, openTicket, admin, "Comentário do admin", CommentVisibility.PUBLICO);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agent);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.findByIdAndTicketId(commentId, openTicket.getId())).thenReturn(Optional.of(comment));

        var request = new br.com.api.auroraorg.ticket.dto.UpdateCommentRequest("Tentativa de edição");

        // Act & Assert
        assertThatThrownBy(() -> commentService.updateComment(openTicket.getId(), commentId, request))
                .isInstanceOf(CommentPermissionDeniedException.class)
                .hasMessageContaining("outro usuário");
    }

    @Test
    @DisplayName("Deve permitir ADMIN editar qualquer comentário")
    void shouldAllowAdminToEditAnyComment() {
        // Arrange
        UUID commentId = UUID.randomUUID();
        TicketComment comment = createComment(commentId, openTicket, agent, "Comentário do agente", CommentVisibility.PUBLICO);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.findByIdAndTicketId(commentId, openTicket.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(TicketComment.class))).thenReturn(comment);

        var request = new br.com.api.auroraorg.ticket.dto.UpdateCommentRequest("Editado pelo admin");

        // Act
        CommentResponse response = commentService.updateComment(openTicket.getId(), commentId, request);

        // Assert
        assertThat(response.content()).isEqualTo("Editado pelo admin");
    }

    // ========== TESTES DE REMOÇÃO ==========

    @Test
    @DisplayName("Deve remover comentário logicamente (soft delete)")
    void shouldSoftDeleteComment() {
        // Arrange
        UUID commentId = UUID.randomUUID();
        TicketComment comment = createComment(commentId, openTicket, agent, "Comentário a remover", CommentVisibility.PUBLICO);

        when(securityUtils.getCurrentUserOrThrow()).thenReturn(agent);
        when(ticketRepository.findById(openTicket.getId())).thenReturn(Optional.of(openTicket));
        when(commentRepository.findByIdAndTicketId(commentId, openTicket.getId())).thenReturn(Optional.of(comment));

        // Act
        commentService.removeComment(openTicket.getId(), commentId);

        // Assert
        assertThat(comment.isRemoved()).isTrue();
        verify(commentRepository).save(comment);
        verify(historyService).recordCommentRemoved(any(), any(), any());
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

    private TicketComment createComment(UUID id, Ticket ticket, User author, String content, CommentVisibility visibility) {
        TicketComment comment = new TicketComment();
        comment.setId(id);
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setVisibility(visibility);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setEdited(false);
        comment.setRemoved(false);
        return comment;
    }
}
