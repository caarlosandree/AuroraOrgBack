package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.SlaTipo;
import br.com.api.auroraorg.ticket.enums.TicketEventType;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.exception.*;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.dto.UserResponse;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de regras de negócio do SLA.
 *
 * Responsabilidades:
 * - Inicializar SLA ao criar chamado
 * - Registrar primeira resposta
 * - Registrar resolução
 * - Recalcular SLA quando prioridade mudar
 * - Atualizar status de SLA
 * - Verificar se chamado está em risco
 * - Verificar se chamado está vencido
 * - Calcular tempo restante/excedido
 * - Registrar eventos no histórico
 *
 * Integrações:
 * - TicketService: ao criar/alterar chamado
 * - TicketCommentService: ao comentar
 * - ChamadoAtribuicaoService: ao assumir/atribuir
 * - TicketHistoryService: eventos de timeline
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaService {

    private final TicketRepository ticketRepository;
    private final SlaCalculadoraService slaCalculadora;
    private final TicketHistoryService historyService;
    private final SecurityUtils securityUtils;

    // ========== INICIALIZAÇÃO ==========

    /**
     * Inicializa SLA completo ao criar um chamado.
     * Chamado por TicketService.createTicket().
     */
    @Transactional
    public void inicializarSla(Ticket ticket) {
        slaCalculadora.inicializarSla(ticket);
        log.info("SLA inicializado para chamado {}. PR: {}, Resolução: {}",
                ticket.getId(), ticket.getPrazoPrimeiraResposta(), ticket.getPrazoResolucao());
    }

    // ========== PRIMEIRA RESPOSTA ==========

    /**
     * Registra primeira resposta ao chamado.
     * Pode ser acionado por:
     * - Agente/Admin comentando pela primeira vez
     * - Agente assumindo o chamado
     * - Status mudando para EM_ATENDIMENTO
     *
     * Não registra se:
     * - Já houve primeira resposta
     * - Usuário é SOLICITANTE
     * - Chamado está cancelado/fechado/resolvido
     */
    @Transactional
    public void registrarPrimeiraResposta(UUID chamadoId, User agente) {
        if (agente.getRole() == UserRole.SOLICITANTE) {
            return; // Comentário do solicitante não conta
        }

        Ticket ticket = findTicketOrThrow(chamadoId);

        if (!ticket.isSlaMonitored()) {
            log.debug("Chamado {} não permite registrar primeira resposta (status={})",
                    chamadoId, ticket.getStatus());
            return;
        }

        if (ticket.hasPrimeiraResposta()) {
            log.debug("Primeira resposta já registrada para chamado {} por {}",
                    chamadoId, ticket.getPrimeiraRespostaPor().getEmail());
            return;
        }

        ticket.registrarPrimeiraResposta(agente);
        slaCalculadora.atualizarStatusPrimeiraResposta(ticket);

        Ticket updated = ticketRepository.save(ticket);

        log.info("Primeira resposta registrada para chamado {} por {}. Status PR: {}",
                chamadoId, agente.getEmail(), updated.getSlaPrimeiraRespostaStatus());

        // Evento de histórico
        historyService.recordSlaPrimeiraRespostaRegistrada(updated, agente);
    }

    // ========== RESOLUÇÃO ==========

    /**
     * Registra resolução do chamado e calcula status do SLA.
     * Chamado quando status muda para RESOLVIDO.
     */
    @Transactional
    public void registrarResolucao(UUID chamadoId) {
        Ticket ticket = findTicketOrThrow(chamadoId);

        ticket.registrarResolucao();
        slaCalculadora.atualizarStatusResolucao(ticket);

        Ticket updated = ticketRepository.save(ticket);

        log.info("Resolução registrada para chamado {}. Status SLA: {}",
                chamadoId, updated.getSlaResolucaoStatus());

        // Evento de histórico
        historyService.recordSlaResolucaoRegistrada(updated);
    }

    // ========== CANCELAMENTO ==========

    /**
     * Marca SLA como cancelado quando o chamado é cancelado.
     */
    @Transactional
    public void cancelarSla(UUID chamadoId) {
        Ticket ticket = findTicketOrThrow(chamadoId);

        ticket.setSlaPrimeiraRespostaStatus(SlaStatus.CANCELADO);
        ticket.setSlaResolucaoStatus(SlaStatus.CANCELADO);

        Ticket updated = ticketRepository.save(ticket);

        log.info("SLA cancelado para chamado {}", chamadoId);

        historyService.recordSlaCancelado(updated);
    }

    // ========== RECÁLCULO ==========

    /**
     * Recalcula SLA quando a prioridade muda.
     * Regra MVP: recalcula apenas se chamado ainda não foi resolvido/fechado/cancelado
     * e se a primeira resposta ainda não ocorreu (opcional).
     *
     * Apenas ADMIN pode recalcular manualmente.
     */
    @Transactional
    public ChamadoSlaResponse recalcularSla(UUID chamadoId, RecalcularSlaRequest request) {
        if (!securityUtils.isAdmin()) {
            throw new TicketPermissionDeniedException("recalcular SLA",
                    "Apenas ADMIN pode recalcular SLA manualmente");
        }

        Ticket ticket = findTicketOrThrow(chamadoId);

        if (!ticket.allowsSlaUpdate()) {
            throw new SlaNaoPodeSerRecalculadoException(chamadoId);
        }

        TicketPriority novaPrioridade = ticket.getPriority();
        TicketPriority prioridadeAnterior = ticket.getPriority(); // Ajustar se necessário

        slaCalculadora.recalcularPrazos(ticket, novaPrioridade);
        ticket.setSlaDueAt(ticket.getPrazoResolucao());

        Ticket updated = ticketRepository.save(ticket);

        log.info("SLA recalculado manualmente para chamado {}. Motivo: {}",
                chamadoId, request.motivo());

        historyService.recordSlaRecalculado(updated, prioridadeAnterior, novaPrioridade, request.motivo());

        return buildChamadoSlaResponse(updated);
    }

    /**
     * Recalcula SLA automaticamente quando a prioridade é alterada.
     * Chamado por TicketService ao detectar mudança de prioridade.
     */
    @Transactional
    public void recalcularSlaPorMudancaPrioridade(Ticket ticket, TicketPriority prioridadeAnterior) {
        if (!ticket.allowsSlaUpdate()) {
            log.debug("Chamado {} não permite recálculo de SLA (status terminal)", ticket.getId());
            return;
        }

        slaCalculadora.recalcularPrazos(ticket, ticket.getPriority());
        ticket.setSlaDueAt(ticket.getPrazoResolucao());

        log.info("SLA recalculado automaticamente para chamado {}. Prioridade: {} -> {}",
                ticket.getId(), prioridadeAnterior, ticket.getPriority());

        historyService.recordSlaRecalculado(ticket, prioridadeAnterior, ticket.getPriority(),
                "Alteração de prioridade do chamado");
    }

    // ========== CONSULTA ==========

    @Transactional(readOnly = true)
    public ChamadoSlaResponse consultarSla(UUID chamadoId) {
        Ticket ticket = findTicketOrThrow(chamadoId);
        validarPermissaoConsulta(ticket);
        return buildChamadoSlaResponse(ticket);
    }

    // ========== CONSTRUÇÃO DE DTO ==========

    private ChamadoSlaResponse buildChamadoSlaResponse(Ticket ticket) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime criadoEm = ticket.getCreatedAt();

        boolean chamadoVencido = ticket.getSlaResolucaoStatus() == SlaStatus.VENCIDO
                || ticket.getSlaPrimeiraRespostaStatus() == SlaStatus.VENCIDO;

        boolean chamadoEmRisco = ticket.getSlaResolucaoStatus() == SlaStatus.EM_RISCO
                || ticket.getSlaPrimeiraRespostaStatus() == SlaStatus.EM_RISCO;

        SlaResumoResponse slaPrimeiraResposta = buildSlaResumo(
                SlaTipo.PRIMEIRA_RESPOSTA,
                ticket.getSlaPrimeiraRespostaStatus(),
                ticket.getPrazoPrimeiraResposta(),
                ticket.getDataPrimeiraResposta(),
                criadoEm,
                agora);

        SlaResumoResponse slaResolucao = buildSlaResumo(
                SlaTipo.RESOLUCAO,
                ticket.getSlaResolucaoStatus(),
                ticket.getPrazoResolucao(),
                ticket.getDataResolucao(),
                criadoEm,
                agora);

        UserResponse primeiraRespostaPor = ticket.getPrimeiraRespostaPor() != null
                ? new UserResponse(
                        ticket.getPrimeiraRespostaPor().getId(),
                        ticket.getPrimeiraRespostaPor().getName(),
                        ticket.getPrimeiraRespostaPor().getEmail(),
                        ticket.getPrimeiraRespostaPor().getRole(),
                        ticket.getPrimeiraRespostaPor().getActive(),
                        ticket.getPrimeiraRespostaPor().getCreatedAt(),
                        ticket.getPrimeiraRespostaPor().getUpdatedAt())
                : null;

        return new ChamadoSlaResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                slaPrimeiraResposta,
                slaResolucao,
                primeiraRespostaPor,
                chamadoVencido,
                chamadoEmRisco,
                criadoEm
        );
    }

    private SlaResumoResponse buildSlaResumo(SlaTipo tipo, SlaStatus status,
                                            LocalDateTime prazo, LocalDateTime dataConclusao,
                                            LocalDateTime criadoEm, LocalDateTime agora) {
        Long tempoDecorrido = null;
        Long tempoRestante = null;
        Long tempoExcedido = null;
        Double percentualConsumido = null;
        Boolean emRisco = null;

        if (dataConclusao != null) {
            // Já concluído
            tempoDecorrido = java.time.Duration.between(criadoEm, dataConclusao).getSeconds();
            percentualConsumido = 100.0;
            emRisco = false;
        } else if (prazo != null) {
            // Ainda aberto
            tempoDecorrido = java.time.Duration.between(criadoEm, agora).getSeconds();
            if (agora.isAfter(prazo)) {
                tempoExcedido = java.time.Duration.between(prazo, agora).getSeconds();
                tempoRestante = 0L;
            } else {
                tempoRestante = java.time.Duration.between(agora, prazo).getSeconds();
            }
            percentualConsumido = slaCalculadora.calcularPercentualConsumido(criadoEm, prazo, agora);
            emRisco = slaCalculadora.isEmRiscoPadrao(criadoEm, prazo, agora);
        }

        return new SlaResumoResponse(
                tipo,
                tipo.getLabel(),
                status,
                status.getLabel(),
                prazo,
                dataConclusao,
                tempoDecorrido,
                tempoRestante,
                tempoExcedido,
                percentualConsumido,
                emRisco
        );
    }

    // ========== PERMISSÕES ==========

    private void validarPermissaoConsulta(Ticket ticket) {
        User currentUser = securityUtils.getCurrentUserOrThrow();
        UserRole role = currentUser.getRole();

        // ADMIN, GESTOR e AGENTE podem consultar
        if (role == UserRole.ADMIN || role == UserRole.GESTOR || role == UserRole.AGENTE) {
            return;
        }

        // SOLICITANTE só consulta próprio chamado (se regra do produto permitir)
        if (role == UserRole.SOLICITANTE && !ticket.isRequestedBy(currentUser)) {
            throw new TicketPermissionDeniedException("consultar SLA",
                    "Você só pode consultar SLA dos seus próprios chamados");
        }
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }
}
