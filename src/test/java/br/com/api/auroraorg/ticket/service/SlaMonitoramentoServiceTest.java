package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaMonitoramentoServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SlaCalculadoraService slaCalculadora;

    @Mock
    private TicketHistoryService historyService;

    @InjectMocks
    private SlaMonitoramentoService monitoramentoService;

    private LocalDateTime agora;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.of(2026, 1, 15, 10, 0);
    }

    private Ticket criarChamadoAberto(SlaStatus slaResolucao, SlaStatus slaPrimeiraResposta,
                                     LocalDateTime prazoPrimeiraResposta, LocalDateTime prazoResolucao) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .title("Teste")
                .status(TicketStatus.EM_ATENDIMENTO)
                .priority(TicketPriority.MEDIA)
                .createdAt(agora.minusHours(10))
                .prazoPrimeiraResposta(prazoPrimeiraResposta)
                .prazoResolucao(prazoResolucao)
                .slaPrimeiraRespostaStatus(slaPrimeiraResposta)
                .slaResolucaoStatus(slaResolucao)
                .build();
    }

    // ========== MONITORAMENTO AUTOMÁTICO ==========

    @Test
    void executarMonitoramento_chamadoDentroDoPrazo_deveManterStatus() {
        Ticket ticket = criarChamadoAberto(
                SlaStatus.DENTRO_DO_PRAZO,
                SlaStatus.DENTRO_DO_PRAZO,
                agora.plusHours(2),   // PR: dentro
                agora.plusHours(38)  // Resolução: dentro
        );

        when(ticketRepository.findOpenTickets()).thenReturn(List.of(ticket));
        when(slaCalculadora.calcularStatusAberto(ticket.getPrazoResolucao(), agora)).thenReturn(SlaStatus.DENTRO_DO_PRAZO);
        when(slaCalculadora.calcularStatusAberto(ticket.getPrazoPrimeiraResposta(), agora)).thenReturn(SlaStatus.DENTRO_DO_PRAZO);

        monitoramentoService.executarMonitoramento();

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void executarMonitoramento_resolucaoVenceu_deveMarcarVencido() {
        Ticket ticket = criarChamadoAberto(
                SlaStatus.DENTRO_DO_PRAZO,
                SlaStatus.DENTRO_DO_PRAZO,
                agora.plusHours(2),
                agora.minusHours(1)  // Já venceu
        );

        when(ticketRepository.findOpenTickets()).thenReturn(List.of(ticket));
        when(slaCalculadora.calcularStatusAberto(eq(ticket.getPrazoResolucao()), any())).thenReturn(SlaStatus.VENCIDO);
        when(slaCalculadora.calcularStatusAberto(eq(ticket.getPrazoPrimeiraResposta()), any())).thenReturn(SlaStatus.DENTRO_DO_PRAZO);
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        monitoramentoService.executarMonitoramento();

        assertThat(ticket.getSlaResolucaoStatus()).isEqualTo(SlaStatus.VENCIDO);
        verify(historyService).recordSlaResolucaoVencido(ticket);
    }

    @Test
    void executarMonitoramento_primeiraRespostaVenceu_deveMarcarVencido() {
        Ticket ticket = criarChamadoAberto(
                SlaStatus.DENTRO_DO_PRAZO,
                SlaStatus.DENTRO_DO_PRAZO,
                agora.minusHours(1),  // PR venceu
                agora.plusHours(38)
        );

        when(ticketRepository.findOpenTickets()).thenReturn(List.of(ticket));
        when(slaCalculadora.calcularStatusAberto(eq(ticket.getPrazoPrimeiraResposta()), any())).thenReturn(SlaStatus.VENCIDO);
        when(slaCalculadora.calcularStatusAberto(eq(ticket.getPrazoResolucao()), any())).thenReturn(SlaStatus.DENTRO_DO_PRAZO);
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        monitoramentoService.executarMonitoramento();

        assertThat(ticket.getSlaPrimeiraRespostaStatus()).isEqualTo(SlaStatus.VENCIDO);
        verify(historyService).recordSlaPrimeiraRespostaVencido(ticket);
    }

    @Test
    void executarMonitoramento_semChamadosAbertos_deveNaoFazerNada() {
        when(ticketRepository.findOpenTickets()).thenReturn(List.of());

        monitoramentoService.executarMonitoramento();

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void executarMonitoramento_chamadoResolvido_deveIgnorar() {
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .status(TicketStatus.RESOLVIDO)
                .slaResolucaoStatus(SlaStatus.CUMPRIDO)
                .build();

        when(ticketRepository.findOpenTickets()).thenReturn(List.of());

        monitoramentoService.executarMonitoramento();

        verify(ticketRepository, never()).save(any());
    }

    // ========== BUSCAS ==========

    @Test
    void buscarChamadosVencidos_deveRetornarLista() {
        when(ticketRepository.findOverdueTickets(any())).thenReturn(List.of());

        var resultado = monitoramentoService.buscarChamadosVencidos();

        assertThat(resultado).isEmpty();
    }

    // ========== DASHBOARD ==========

    @Test
    void gerarResumoDashboard_comDiversosStatus_deveRetornarResumo() {
        Ticket dentroDoPrazo = Ticket.builder()
                .priority(TicketPriority.BAIXA)
                .slaResolucaoStatus(SlaStatus.DENTRO_DO_PRAZO)
                .slaPrimeiraRespostaStatus(SlaStatus.DENTRO_DO_PRAZO)
                .build();

        Ticket vencido = Ticket.builder()
                .priority(TicketPriority.ALTA)
                .slaResolucaoStatus(SlaStatus.VENCIDO)
                .slaPrimeiraRespostaStatus(SlaStatus.VENCIDO)
                .build();

        Ticket cumprido = Ticket.builder()
                .priority(TicketPriority.MEDIA)
                .slaResolucaoStatus(SlaStatus.CUMPRIDO)
                .slaPrimeiraRespostaStatus(SlaStatus.CUMPRIDO)
                .build();

        Ticket violado = Ticket.builder()
                .priority(TicketPriority.CRITICA)
                .slaResolucaoStatus(SlaStatus.VIOLADO)
                .slaPrimeiraRespostaStatus(SlaStatus.VIOLADO)
                .build();

        when(ticketRepository.findAll()).thenReturn(List.of(dentroDoPrazo, vencido, cumprido, violado));

        var dashboard = monitoramentoService.gerarResumoDashboard();

        assertThat(dashboard.totalChamados()).isEqualTo(4);
        assertThat(dashboard.chamadosDentroDoPrazo()).isEqualTo(1);
        assertThat(dashboard.chamadosVencidos()).isEqualTo(1);
        assertThat(dashboard.chamadosCumpridos()).isEqualTo(1);
        assertThat(dashboard.chamadosViolados()).isEqualTo(1);
    }
}
