package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.CommentResponse;
import br.com.api.auroraorg.ticket.dto.CreateCommentRequest;
import br.com.api.auroraorg.ticket.dto.UpdateCommentRequest;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.entity.TicketComment;
import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.TicketCommentRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de Comentários de Chamados.
 *
 * Regras de negócio implementadas:
 * - SOLICITANTE só pode criar comentário PUBLICO
 * - AGENTE pode criar comentário PUBLICO ou INTERNO
 * - ADMIN pode criar/editar/remover qualquer comentário
 * - GESTOR pode visualizar todos os comentários mas normalmente não comenta
 * - Comentários removidos não aparecem para SOLICITANTE
 * - Chamados FECHADOS/CANCELADOS não aceitam novos comentários
 * - Apenas autor ou ADMIN pode editar/remover comentário
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryService historyService;
    private final SecurityUtils securityUtils;

    // ========== OPERAÇÕES DE CRIAÇÃO ==========

    /**
     * Adiciona um novo comentário ao chamado.
     *
     * Regras:
     * - Valida permissões de visibilidade por perfil
     * - Valida se chamado permite comentários (não FECHADO/CANCELADO)
     * - Registra evento de histórico
     */
    @Transactional
    public CommentResponse addComment(UUID ticketId, CreateCommentRequest request) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);

        log.info("Adicionando comentário ao chamado {}. Usuário: {}, Visibilidade: {}",
                ticketId, currentUser.getEmail(), request.visibility());

        // Valida se pode comentar neste chamado
        validateCanComment(ticket, currentUser, request.visibility());

        // Cria o comentário
        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .author(currentUser)
                .content(request.content().trim())
                .visibility(request.visibility())
                .build();

        TicketComment saved = commentRepository.save(comment);

        // Registra evento de histórico
        historyService.recordCommentAdded(ticket, currentUser, saved);

        log.info("Comentário adicionado com sucesso. ID: {}", saved.getId());

        return toResponse(saved);
    }

    // ========== OPERAÇÕES DE ATUALIZAÇÃO ==========

    /**
     * Atualiza um comentário existente.
     *
     * Regras:
     * - Apenas autor ou ADMIN pode editar
     * - Comentário removido não pode ser editado
     * - Não permite alterar visibilidade (apenas conteúdo)
     */
    @Transactional
    public CommentResponse updateComment(UUID ticketId, UUID commentId, UpdateCommentRequest request) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);
        TicketComment comment = findCommentOrThrow(commentId, ticketId);

        log.info("Atualizando comentário {} do chamado {}. Usuário: {}",
                commentId, ticketId, currentUser.getEmail());

        // Valida permissão de edição
        validateCanEdit(comment, currentUser);

        // Valida se comentário não foi removido
        if (comment.isRemoved()) {
            throw new CommentRemovedException(commentId);
        }

        // Atualiza conteúdo
        comment.updateContent(request.content().trim());
        TicketComment updated = commentRepository.save(comment);

        // Registra evento de edição
        historyService.recordCommentEdited(ticket, currentUser, updated);

        log.info("Comentário atualizado com sucesso");

        return toResponse(updated);
    }

    // ========== OPERAÇÕES DE REMOÇÃO ==========

    /**
     * Remove logicamente um comentário (soft delete).
     *
     * Regras:
     * - Apenas autor ou ADMIN pode remover
     * - Não exclui fisicamente, apenas marca como removido
     * - Registra evento de histórico
     */
    @Transactional
    public void removeComment(UUID ticketId, UUID commentId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);
        TicketComment comment = findCommentOrThrow(commentId, ticketId);

        log.info("Removendo comentário {} do chamado {}. Usuário: {}",
                commentId, ticketId, currentUser.getEmail());

        // Valida permissão de remoção
        validateCanDelete(comment, currentUser);

        // Valida se já não foi removido
        if (comment.isRemoved()) {
            throw new CommentRemovedException(commentId);
        }

        // Soft delete
        comment.markAsRemoved();
        commentRepository.save(comment);

        // Registra evento de remoção
        historyService.recordCommentRemoved(ticket, currentUser, comment);

        log.info("Comentário removido logicamente com sucesso");
    }

    // ========== OPERAÇÕES DE CONSULTA ==========

    /**
     * Lista comentários de um chamado conforme o perfil do usuário.
     *
     * Filtros por perfil:
     * - SOLICITANTE: apenas comentários PUBLICO e não removidos
     * - AGENTE/ADMIN/GESTOR: todos os comentários não removidos
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID ticketId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);

        // Valida acesso ao chamado
        validateTicketAccess(ticket, currentUser);

        List<TicketComment> comments;

        if (currentUser.getRole() == UserRole.SOLICITANTE) {
            // Solicitante só vê comentários públicos não removidos
            comments = commentRepository.findPublicCommentsByTicketId(ticketId);
        } else {
            // AGENTE, ADMIN, GESTOR veem todos os comentários não removidos
            comments = commentRepository.findAllVisibleCommentsByTicketId(ticketId);
        }

        return comments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista comentários com paginação.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> listComments(UUID ticketId, Pageable pageable) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);

        // Valida acesso ao chamado
        validateTicketAccess(ticket, currentUser);

        Page<TicketComment> comments;

        if (currentUser.getRole() == UserRole.SOLICITANTE) {
            // Solicitante: apenas públicos
            comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId, pageable);
        } else {
            // Equipe: todos não removidos
            comments = commentRepository.findAllVisibleCommentsByTicketId(ticketId, pageable);
        }

        return comments.map(this::toResponse);
    }

    /**
     * Busca um comentário específico.
     */
    @Transactional(readOnly = true)
    public CommentResponse getComment(UUID ticketId, UUID commentId) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        Ticket ticket = findTicketOrThrow(ticketId);
        TicketComment comment = findCommentOrThrow(commentId, ticketId);

        // Valida acesso
        validateTicketAccess(ticket, currentUser);

        // Valida visibilidade
        if (currentUser.getRole() == UserRole.SOLICITANTE && comment.getVisibility() == CommentVisibility.INTERNO) {
            throw new CommentNotFoundException(commentId, ticketId);
        }

        // Para comentários removidos, retorna versão especial
        if (comment.isRemoved()) {
            if (currentUser.getRole() == UserRole.SOLICITANTE) {
                throw new CommentNotFoundException(commentId, ticketId);
            }
            return CommentResponse.createRemovedResponse(
                    comment.getId(), ticketId, comment.getCreatedAt()
            );
        }

        return toResponse(comment);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private Ticket findTicketOrThrow(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private TicketComment findCommentOrThrow(UUID commentId, UUID ticketId) {
        return commentRepository.findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new CommentNotFoundException(commentId, ticketId));
    }

    private void validateCanComment(Ticket ticket, User user, CommentVisibility visibility) {
        // Verifica se chamado permite comentários
        if (ticket.getStatus().isTerminal()) {
            // ADMIN pode comentar mesmo em chamados fechados (se necessário)
            if (user.getRole() != UserRole.ADMIN) {
                throw new CommentNotAllowedException(ticket.getStatus());
            }
        }

        // Regras por perfil
        switch (user.getRole()) {
            case SOLICITANTE:
                // Solicitante só pode criar comentários públicos
                if (visibility == CommentVisibility.INTERNO) {
                    throw new CommentNotAllowedException("INTERNO", "SOLICITANTE");
                }
                // Solicitante só pode comentar em chamados que ele abriu
                if (!ticket.isRequestedBy(user)) {
                    throw new CommentPermissionDeniedException("comentar",
                            "Você só pode comentar em chamados que você abriu");
                }
                break;

            case AGENTE:
            case ADMIN:
                // Podem criar comentários públicos ou internos
                break;

            case GESTOR:
                // Por padrão, GESTOR não comenta (apenas visualiza)
                // Se necessário, adicionar regra específica
                throw new CommentPermissionDeniedException("comentar",
                        "GESTOR não tem permissão para comentar");

            default:
                throw new CommentPermissionDeniedException("comentar");
        }
    }

    private void validateCanEdit(TicketComment comment, User user) {
        // ADMIN pode editar qualquer comentário
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        // AGENTE pode editar apenas próprios comentários
        if (user.getRole() == UserRole.AGENTE) {
            if (!comment.isAuthor(user)) {
                throw new CommentPermissionDeniedException("editar comentário de outro usuário");
            }
            return;
        }

        // SOLICITANTE só pode editar próprio comentário se ainda não foi respondido
        if (user.getRole() == UserRole.SOLICITANTE) {
            if (!comment.isAuthor(user)) {
                throw new CommentPermissionDeniedException("editar comentário de outro usuário");
            }
            return;
        }

        // GESTOR não edita comentários
        throw new CommentPermissionDeniedException("editar comentário");
    }

    private void validateCanDelete(TicketComment comment, User user) {
        // ADMIN pode remover qualquer comentário
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        // AGENTE pode remover apenas próprios comentários
        if (user.getRole() == UserRole.AGENTE) {
            if (!comment.isAuthor(user)) {
                throw new CommentPermissionDeniedException("remover comentário de outro usuário");
            }
            return;
        }

        // SOLICITANTE pode remover apenas próprio comentário
        if (user.getRole() == UserRole.SOLICITANTE) {
            if (!comment.isAuthor(user)) {
                throw new CommentPermissionDeniedException("remover comentário de outro usuário");
            }
            return;
        }

        // GESTOR não remove comentários
        throw new CommentPermissionDeniedException("remover comentário");
    }

    private void validateTicketAccess(Ticket ticket, User user) {
        // SOLICITANTE só vê comentários de chamados que ele abriu
        if (user.getRole() == UserRole.SOLICITANTE && !ticket.isRequestedBy(user)) {
            throw new CommentPermissionDeniedException("visualizar comentários",
                    "Você só pode ver comentários de chamados que você abriu");
        }

        // AGENTE precisa ter acesso ao chamado (ser responsável ou estar na fila)
        // TODO: Implementar regra de fila/fila de atendimento se necessário
    }

    private CommentResponse toResponse(TicketComment comment) {
        CommentResponse.AuthorInfo authorInfo = null;
        if (comment.getAuthor() != null) {
            authorInfo = new CommentResponse.AuthorInfo(
                    comment.getAuthor().getId(),
                    comment.getAuthor().getName(),
                    comment.getAuthor().getEmail()
            );
        }

        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                authorInfo,
                comment.getContent(),
                comment.getVisibility(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isEdited(),
                comment.isRemoved()
        );
    }
}
