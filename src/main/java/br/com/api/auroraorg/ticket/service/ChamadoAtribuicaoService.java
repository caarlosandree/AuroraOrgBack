package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Categoria;
import br.com.api.auroraorg.ticket.entity.Fila;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.CategoriaRepository;
import br.com.api.auroraorg.ticket.repository.FilaAgenteRepository;
import br.com.api.auroraorg.ticket.repository.FilaRepository;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import br.com.api.auroraorg.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoAtribuicaoService {

    private final TicketRepository ticketRepository;
    private final FilaRepository filaRepository;
    private final CategoriaRepository categoriaRepository;
    private final FilaAgenteRepository filaAgenteRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final TicketHistoryService historyService;
    private final SlaService slaService;

    @Transactional
    public ChamadoAtribuicaoResponse transferirFila(UUID chamadoId, TransferirFilaChamadoRequest request) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyAssignment(ticket);
        validateIsAdminOrManager(currentUser, "transferir fila do chamado");

        Fila novaFila = filaRepository.findById(request.filaId())
                .orElseThrow(() -> new FilaNotFoundException(request.filaId()));

        Fila filaAnterior = ticket.getFila();

        // Se há responsável atual, verificar se pertence à nova fila
        if (ticket.hasAssignee()) {
            boolean responsavelNaNovaFila = filaAgenteRepository.isAgenteInFila(
                    novaFila.getId(), ticket.getAssignee().getId());
            if (!responsavelNaNovaFila) {
                throw new ResponsavelNaoPertenceFilaException();
            }
        }

        ticket.setFila(novaFila);
        Ticket updated = ticketRepository.save(ticket);

        historyService.recordFilaAlterada(updated, currentUser,
                filaAnterior != null ? filaAnterior.getName() : null,
                novaFila.getName());

        log.info("Chamado {} transferido da fila {} para {} por {}",
                chamadoId,
                filaAnterior != null ? filaAnterior.getName() : "nenhuma",
                novaFila.getName(),
                currentUser.getEmail());

        return toAtribuicaoResponse(updated, "Chamado transferido para fila " + novaFila.getName());
    }

    @Transactional
    public ChamadoAtribuicaoResponse alterarCategoria(UUID chamadoId, AlterarCategoriaChamadoRequest request) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyAssignment(ticket);
        validateIsAdminOrManager(currentUser, "alterar categoria do chamado");

        Categoria novaCategoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new CategoriaNotFoundException(request.categoriaId()));

        Categoria categoriaAnterior = ticket.getCategoria();
        String categoriaAnteriorNome = categoriaAnterior != null ? categoriaAnterior.getName() : null;

        ticket.setCategoria(novaCategoria);
        ticket.setCategory(novaCategoria.getName());

        // Se a nova categoria tem fila padrão e o chamado não está em nenhuma fila,
        // ou queremos sempre aplicar fila padrão? Regra: fila padrão só na criação.
        // Não alteramos fila aqui automaticamente.

        Ticket updated = ticketRepository.save(ticket);

        historyService.recordCategoriaAlterada(updated, currentUser,
                categoriaAnteriorNome,
                novaCategoria.getName());

        log.info("Chamado {} categoria alterada de {} para {} por {}",
                chamadoId, categoriaAnteriorNome, novaCategoria.getName(), currentUser.getEmail());

        return toAtribuicaoResponse(updated, "Categoria alterada para " + novaCategoria.getName());
    }

    @Transactional
    public ChamadoAtribuicaoResponse atribuirResponsavel(UUID chamadoId, AtribuirResponsavelRequest request) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyAssignment(ticket);

        if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
            throw new TicketPermissionDeniedException("atribuir chamado a outro usuário",
                    "Apenas ADMIN ou GESTOR podem atribuir chamados");
        }

        User novoResponsavel = userRepository.findById(request.responsavelId())
                .orElseThrow(() -> new br.com.api.auroraorg.shared.exception.InvalidCredentialsException());

        if (!canAttendTickets(novoResponsavel)) {
            throw new InvalidTicketOperationException(
                    "O usuário atribuído deve ter perfil AGENTE, ADMIN ou GESTOR");
        }

        // Se chamado está em uma fila, o responsável deve pertencer à fila
        if (ticket.getFila() != null) {
            boolean naFila = filaAgenteRepository.isAgenteInFila(ticket.getFila().getId(), novoResponsavel.getId());
            if (!naFila && novoResponsavel.getRole() != UserRole.ADMIN) {
                throw new InvalidTicketOperationException(
                        "O responsável deve pertencer à fila do chamado ou ser ADMIN");
            }
        }

        User responsavelAnterior = ticket.getAssignee();
        ticket.setAssignee(novoResponsavel);

        if (ticket.getStatus() == TicketStatus.ABERTO) {
            ticket.setStatus(TicketStatus.EM_TRIAGEM);
        }

        Ticket updated = ticketRepository.save(ticket);

        if (responsavelAnterior == null) {
            historyService.recordResponsavelAtribuido(updated, currentUser, novoResponsavel);
        } else {
            historyService.recordResponsavelAlterado(updated, currentUser, responsavelAnterior, novoResponsavel);
        }

        log.info("Chamado {} atribuído a {} por {}", chamadoId, novoResponsavel.getEmail(), currentUser.getEmail());

        return toAtribuicaoResponse(updated, "Responsável atribuído: " + novoResponsavel.getName());
    }

    @Transactional
    public ChamadoAtribuicaoResponse assumirChamado(UUID chamadoId) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyAssignment(ticket);

        if (!canAttendTickets(currentUser)) {
            throw new TicketPermissionDeniedException("assumir chamado",
                    "Apenas AGENTE, ADMIN ou GESTOR podem assumir chamados");
        }

        // Se chamado está em uma fila, o agente deve pertencer à fila (exceto ADMIN/GESTOR)
        if (ticket.getFila() != null && currentUser.getRole() == UserRole.AGENTE) {
            boolean naFila = filaAgenteRepository.isAgenteInFila(ticket.getFila().getId(), currentUser.getId());
            if (!naFila) {
                throw new TicketPermissionDeniedException("assumir chamado",
                        "Você não pertence à fila deste chamado");
            }
        }

        ticket.setAssignee(currentUser);

        if (ticket.getStatus() == TicketStatus.ABERTO || ticket.getStatus() == TicketStatus.EM_TRIAGEM) {
            ticket.setStatus(TicketStatus.EM_ATENDIMENTO);
        }

        Ticket updated = ticketRepository.save(ticket);

        // Registra primeira resposta se ainda não ocorreu
        slaService.registrarPrimeiraResposta(chamadoId, currentUser);

        historyService.recordChamadoAssumido(updated, currentUser);

        log.info("Chamado {} assumido por {}", chamadoId, currentUser.getEmail());

        return toAtribuicaoResponse(updated, "Chamado assumido com sucesso");
    }

    @Transactional
    public ChamadoAtribuicaoResponse removerResponsavel(UUID chamadoId) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        User currentUser = securityUtils.getCurrentUserOrThrow();

        validateCanModifyAssignment(ticket);

        if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
            throw new TicketPermissionDeniedException("remover responsável",
                    "Apenas ADMIN ou GESTOR podem remover responsáveis");
        }

        if (!ticket.hasAssignee()) {
            throw new InvalidTicketOperationException("O chamado não possui responsável para remover");
        }

        User responsavelAnterior = ticket.getAssignee();
        ticket.setAssignee(null);
        Ticket updated = ticketRepository.save(ticket);

        historyService.recordResponsavelRemovido(updated, currentUser, responsavelAnterior);

        log.info("Responsável {} removido do chamado {} por {}",
                responsavelAnterior.getEmail(), chamadoId, currentUser.getEmail());

        return toAtribuicaoResponse(updated, "Responsável removido do chamado");
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private void validateCanModifyAssignment(Ticket ticket) {
        if (ticket.getStatus().isTerminal()) {
            throw new ChamadoNaoPodeSerAtribuidoException(ticket.getStatus());
        }
    }

    private void validateIsAdminOrManager(User user, String action) {
        if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.GESTOR) {
            throw new TicketPermissionDeniedException(action,
                    "Apenas ADMIN ou GESTOR podem realizar esta ação");
        }
    }

    private boolean canAttendTickets(User user) {
        return user.getRole() == UserRole.ADMIN ||
               user.getRole() == UserRole.AGENTE ||
               user.getRole() == UserRole.GESTOR;
    }

    private ChamadoAtribuicaoResponse toAtribuicaoResponse(Ticket ticket, String message) {
        FilaResponse filaResponse = ticket.getFila() != null
                ? new FilaResponse(ticket.getFila().getId(), ticket.getFila().getName(),
                        ticket.getFila().getDescription(), ticket.getFila().getActive(),
                        ticket.getFila().getCreatedAt(), ticket.getFila().getUpdatedAt())
                : null;

        CategoriaResponse categoriaResponse = ticket.getCategoria() != null
                ? new CategoriaResponse(ticket.getCategoria().getId(), ticket.getCategoria().getName(),
                        ticket.getCategoria().getDescription(), ticket.getCategoria().getActive(),
                        ticket.getCategoria().getFilaPadrao() != null
                                ? new FilaResponse(ticket.getCategoria().getFilaPadrao().getId(),
                                        ticket.getCategoria().getFilaPadrao().getName(),
                                        ticket.getCategoria().getFilaPadrao().getDescription(),
                                        ticket.getCategoria().getFilaPadrao().getActive(),
                                        ticket.getCategoria().getFilaPadrao().getCreatedAt(),
                                        ticket.getCategoria().getFilaPadrao().getUpdatedAt())
                                : null,
                        ticket.getCategoria().getCreatedAt(), ticket.getCategoria().getUpdatedAt())
                : null;

        br.com.api.auroraorg.user.dto.UserResponse responsavelResponse = ticket.hasAssignee()
                ? new br.com.api.auroraorg.user.dto.UserResponse(
                        ticket.getAssignee().getId(),
                        ticket.getAssignee().getName(),
                        ticket.getAssignee().getEmail(),
                        ticket.getAssignee().getRole(),
                        ticket.getAssignee().getActive(),
                        ticket.getAssignee().getCreatedAt(),
                        ticket.getAssignee().getUpdatedAt())
                : null;

        return new ChamadoAtribuicaoResponse(
                ticket.getId(),
                ticket.getStatus(),
                filaResponse,
                categoriaResponse,
                responsavelResponse,
                message
        );
    }
}
