package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.SlaDashboardResponse;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de monitoramento automático de SLA.
 *
 * Responsabilidades:
 * - Buscar chamados próximos do vencimento
 * - Buscar chamados vencidos
 * - Marcar chamados em risco
 * - Marcar chamados vencidos
 * - Gerar eventos de histórico para SLA em risco e vencido
 * - Evitar eventos duplicados para o mesmo estado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaMonitoramentoService {

    private final TicketRepository ticketRepository;
    private final SlaCalculadoraService slaCalculadora;
    private final TicketHistoryService historyService;

    private static final Set<SlaStatus> STATUS_MONITORAVEIS = Set.of(
            SlaStatus.DENTRO_DO_PRAZO,
            SlaStatus.EM_RISCO,
            SlaStatus.PAUSADO
    );

    private static final Set<TicketStatus> STATUS_CHAMADO_MONITORAVEIS = Set.of(
            TicketStatus.ABERTO,
            TicketStatus.EM_TRIAGEM,
            TicketStatus.EM_ATENDIMENTO,
            TicketStatus.AGUARDANDO_SOLICITANTE
    );

    // ========== ATUALIZAÇÃO DE STATUS ==========

    /**
     * Executa ciclo de monitoramento:
     * 1. Busca chamados abertos com SLA monitorável
     * 2. Verifica e atualiza status de primeira resposta
     * 3. Verifica e atualiza status de resolução
     * 4. Registra eventos de histórico quando status muda
     */
    @Transactional
    public void executarMonitoramento() {
        log.debug("Iniciando ciclo de monitoramento de SLA...");
        LocalDateTime agora = LocalDateTime.now();

        List<Ticket> chamadosMonitoraveis = buscarChamadosMonitoraveis();
        log.debug("{} chamados monitoráveis encontrados", chamadosMonitoraveis.size());

        for (Ticket ticket : chamadosMonitoraveis) {
            try {
                processarChamado(ticket, agora);
            } catch (Exception e) {
                log.error("Erro ao processar SLA do chamado {}: {}", ticket.getId(), e.getMessage());
            }
        }

        log.debug("Ciclo de monitoramento de SLA concluído");
    }

    private void processarChamado(Ticket ticket, LocalDateTime agora) {
        boolean modificado = false;

        // Monitora primeira resposta (se ainda não ocorreu)
        if (!ticket.hasPrimeiraResposta() && ticket.getPrazoPrimeiraResposta() != null) {
            SlaStatus novoStatus = slaCalculadora.calcularStatusAberto(
                    ticket.getPrazoPrimeiraResposta(), agora);

            if (novoStatus != ticket.getSlaPrimeiraRespostaStatus()) {
                log.info("SLA primeira resposta do chamado {}: {} -> {}",
                        ticket.getId(), ticket.getSlaPrimeiraRespostaStatus(), novoStatus);

                if (novoStatus == SlaStatus.EM_RISCO) {
                    historyService.recordSlaPrimeiraRespostaEmRisco(ticket);
                } else if (novoStatus == SlaStatus.VENCIDO) {
                    historyService.recordSlaPrimeiraRespostaVencido(ticket);
                }

                ticket.setSlaPrimeiraRespostaStatus(novoStatus);
                modificado = true;
            }
        }

        // Monitora resolução (se ainda não resolvido)
        if (ticket.getDataResolucao() == null && ticket.getPrazoResolucao() != null) {
            SlaStatus novoStatus = slaCalculadora.calcularStatusAberto(
                    ticket.getPrazoResolucao(), agora);

            if (novoStatus != ticket.getSlaResolucaoStatus()) {
                log.info("SLA resolução do chamado {}: {} -> {}",
                        ticket.getId(), ticket.getSlaResolucaoStatus(), novoStatus);

                if (novoStatus == SlaStatus.EM_RISCO) {
                    historyService.recordSlaResolucaoEmRisco(ticket);
                } else if (novoStatus == SlaStatus.VENCIDO) {
                    historyService.recordSlaResolucaoVencido(ticket);
                }

                ticket.setSlaResolucaoStatus(novoStatus);
                modificado = true;
            }
        }

        if (modificado) {
            ticketRepository.save(ticket);
        }
    }

    // ========== BUSCAS ==========

    @Transactional(readOnly = true)
    public List<Ticket> buscarChamadosVencidos() {
        LocalDateTime agora = LocalDateTime.now();
        return ticketRepository.findOverdueTickets(agora);
    }

    @Transactional(readOnly = true)
    public List<Ticket> buscarChamadosMonitoraveis() {
        return ticketRepository.findOpenTickets().stream()
                .filter(t -> STATUS_MONITORAVEIS.contains(t.getSlaResolucaoStatus())
                        || (!t.hasPrimeiraResposta() && STATUS_MONITORAVEIS.contains(t.getSlaPrimeiraRespostaStatus())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Ticket> buscarChamadosEmRisco() {
        LocalDateTime agora = LocalDateTime.now();
        return buscarChamadosMonitoraveis().stream()
                .filter(t -> {
                    boolean riscoResolucao = t.getPrazoResolucao() != null
                            && slaCalculadora.isEmRiscoPadrao(t.getCreatedAt(), t.getPrazoResolucao(), agora)
                            && t.getSlaResolucaoStatus() != SlaStatus.VENCIDO;
                    boolean riscoPrimeiraResposta = !t.hasPrimeiraResposta()
                            && t.getPrazoPrimeiraResposta() != null
                            && slaCalculadora.isEmRiscoPadrao(t.getCreatedAt(), t.getPrazoPrimeiraResposta(), agora)
                            && t.getSlaPrimeiraRespostaStatus() != SlaStatus.VENCIDO;
                    return riscoResolucao || riscoPrimeiraResposta;
                })
                .collect(Collectors.toList());
    }

    // ========== DASHBOARD ==========

    @Transactional(readOnly = true)
    public SlaDashboardResponse gerarResumoDashboard() {
        List<Ticket> todos = ticketRepository.findAll();

        long dentroDoPrazo = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.DENTRO_DO_PRAZO
                        || t.getSlaPrimeiraRespostaStatus() == SlaStatus.DENTRO_DO_PRAZO)
                .count();

        long emRisco = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.EM_RISCO
                        || t.getSlaPrimeiraRespostaStatus() == SlaStatus.EM_RISCO)
                .count();

        long vencidos = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.VENCIDO
                        || t.getSlaPrimeiraRespostaStatus() == SlaStatus.VENCIDO)
                .count();

        long cumpridos = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.CUMPRIDO)
                .count();

        long violados = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.VIOLADO)
                .count();

        long cancelados = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.CANCELADO)
                .count();

        Map<String, Long> porPrioridade = todos.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority().getLabel(),
                        Collectors.counting()));

        Map<String, Long> vencidosPorPrioridade = todos.stream()
                .filter(t -> t.getSlaResolucaoStatus() == SlaStatus.VENCIDO)
                .collect(Collectors.groupingBy(
                        t -> t.getPriority().getLabel(),
                        Collectors.counting()));

        long totalResolvidos = cumpridos + violados;
        Double percentualCumprimento = totalResolvidos > 0
                ? (cumpridos * 100.0) / totalResolvidos
                : null;
        Double percentualViolacao = totalResolvidos > 0
                ? (violados * 100.0) / totalResolvidos
                : null;

        return new SlaDashboardResponse(
                todos.size(),
                dentroDoPrazo,
                emRisco,
                vencidos,
                cumpridos,
                violados,
                cancelados,
                percentualCumprimento,
                percentualViolacao,
                porPrioridade,
                vencidosPorPrioridade
        );
    }
}
