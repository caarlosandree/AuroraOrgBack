package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.dto.RecalcularSlaRequest;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.ticket.exception.SlaNaoPodeSerRecalculadoException;
import br.com.api.auroraorg.ticket.exception.TicketNotFoundException;
import br.com.api.auroraorg.ticket.repository.TicketRepository;
import br.com.api.auroraorg.ticket.util.SecurityUtils;
import br.com.api.auroraorg.user.entity.User;
import br.com.api.auroraorg.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SlaCalculadoraService slaCalculadora;

    @Mock
    private TicketHistoryService historyService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SlaService slaService;

    private UUID chamadoId;
    private Ticket ticket;
    private User agente;
    private User admin;
    private User solicitante;
    private LocalDateTime agora;

    @BeforeEach
    void setUp() {
        chamadoId = UUID.randomUUID();
        agora = LocalDateTime.of(2026, 1, 15, 10, 0);

        agente = User.builder()
                .id(UUID.randomUUID())
                .name("Agente Teste")
                .email("agente@teste.com")
                .role(UserRole.AGENTE)
                .build();

        admin = User.builder()
                .id(UUID.randomUUID())
                .name("Admin Teste")
                .email("admin@teste.com")
                .role(UserRole.ADMIN)
                .build();

        solicitante = User.builder()
                .id(UUID.randomUUID())
                .name("Solicitante Teste")
                .email("solicitante@teste.com")
                .role(UserRole.SOLICITANTE)
                .build();

        ticket = Ticket.builder()
                .id(chamadoId)
                .title("Chamado Teste")
                .status(TicketStatus.ABERTO)
                .priority(TicketPriority.MEDIA)
                .createdAt(agora)
                .prazoPrimeiraResposta(agora.plusHours(12))
                .prazoResolucao(agora.plusHours(48))
                .slaPrimeiraRespostaStatus(SlaStatus.DENTRO_DO_PRAZO)
                .slaResolucaoStatus(SlaStatus.DENTRO_DO_PRAZO)
                .build();
    }

    // ========== PRIMEIRA RESPOSTA ==========

    @Test
    void registrarPrimeiraResposta_agenteComSucesso_deveRegistrar() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setSlaPrimeiraRespostaStatus(SlaStatus.CUMPRIDO);
            return null;
        }).when(slaCalculadora).atualizarStatusPrimeiraResposta(any());

        slaService.registrarPrimeiraResposta(chamadoId, agente);

        assertThat(ticket.hasPrimeiraResposta()).isTrue();
        assertThat(ticket.getPrimeiraRespostaPor()).isEqualTo(agente);
        assertThat(ticket.getSlaPrimeiraRespostaStatus()).isEqualTo(SlaStatus.CUMPRIDO);
        verify(historyService).recordSlaPrimeiraRespostaRegistrada(ticket, agente);
    }

    @Test
    void registrarPrimeiraResposta_adminComSucesso_deveRegistrar() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        slaService.registrarPrimeiraResposta(chamadoId, admin);

        assertThat(ticket.hasPrimeiraResposta()).isTrue();
    }

    @Test
    void registrarPrimeiraResposta_solicitante_deveIgnorar() {
        slaService.registrarPrimeiraResposta(chamadoId, solicitante);
        verify(ticketRepository, never()).findById(any());
    }

    @Test
    void registrarPrimeiraResposta_jaRegistrada_deveIgnorar() {
        ticket.registrarPrimeiraResposta(agente);
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));

        slaService.registrarPrimeiraResposta(chamadoId, admin);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void registrarPrimeiraResposta_chamadoTerminal_deveIgnorar() {
        ticket.setStatus(TicketStatus.CANCELADO);
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));

        slaService.registrarPrimeiraResposta(chamadoId, agente);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void registrarPrimeiraResposta_foraDoPrazo_deveMarcarViolado() {
        ticket.setPrazoPrimeiraResposta(agora.minusHours(2));
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setSlaPrimeiraRespostaStatus(SlaStatus.VIOLADO);
            return null;
        }).when(slaCalculadora).atualizarStatusPrimeiraResposta(any());

        slaService.registrarPrimeiraResposta(chamadoId, agente);

        assertThat(ticket.getSlaPrimeiraRespostaStatus()).isEqualTo(SlaStatus.VIOLADO);
    }

    // ========== RESOLUÇÃO ==========

    @Test
    void registrarResolucao_dentroDoPrazo_deveMarcarCumprido() {
        ticket.setPrazoResolucao(agora.plusHours(10));
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setSlaResolucaoStatus(SlaStatus.CUMPRIDO);
            return null;
        }).when(slaCalculadora).atualizarStatusResolucao(any());

        slaService.registrarResolucao(chamadoId);

        assertThat(ticket.getDataResolucao()).isNotNull();
        assertThat(ticket.getSlaResolucaoStatus()).isEqualTo(SlaStatus.CUMPRIDO);
        verify(historyService).recordSlaResolucaoRegistrada(ticket);
    }

    @Test
    void registrarResolucao_foraDoPrazo_deveMarcarViolado() {
        ticket.setPrazoResolucao(agora.minusHours(2));
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setSlaResolucaoStatus(SlaStatus.VIOLADO);
            return null;
        }).when(slaCalculadora).atualizarStatusResolucao(any());

        slaService.registrarResolucao(chamadoId);

        assertThat(ticket.getSlaResolucaoStatus()).isEqualTo(SlaStatus.VIOLADO);
    }

    @Test
    void registrarResolucao_chamadoNaoEncontrado_deveLancarExcecao() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaService.registrarResolucao(chamadoId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // ========== CANCELAMENTO ==========

    @Test
    void cancelarSla_deveMarcarStatusComoCancelado() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        slaService.cancelarSla(chamadoId);

        assertThat(ticket.getSlaPrimeiraRespostaStatus()).isEqualTo(SlaStatus.CANCELADO);
        assertThat(ticket.getSlaResolucaoStatus()).isEqualTo(SlaStatus.CANCELADO);
        verify(historyService).recordSlaCancelado(ticket);
    }

    // ========== RECÁLCULO ==========

    @Test
    void recalcularSla_adminComSucesso_deveRecalcular() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(securityUtils.isAdmin()).thenReturn(true);
        when(ticketRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new RecalcularSlaRequest(false, "Teste de recálculo");
        var response = slaService.recalcularSla(chamadoId, request);

        assertThat(response).isNotNull();
        assertThat(response.chamadoId()).isEqualTo(chamadoId);
    }

    @Test
    void recalcularSla_chamadoTerminal_deveLancarExcecao() {
        ticket.setStatus(TicketStatus.RESOLVIDO);
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(securityUtils.isAdmin()).thenReturn(true);

        var request = new RecalcularSlaRequest(false, "Teste");

        assertThatThrownBy(() -> slaService.recalcularSla(chamadoId, request))
                .isInstanceOf(SlaNaoPodeSerRecalculadoException.class);
    }

    @Test
    void recalcularSlaPorMudancaPrioridade_chamadoAberto_deveRecalcular() {
        ticket.setPriority(TicketPriority.ALTA);
        slaCalculadora.recalcularPrazos(ticket, TicketPriority.ALTA);

        slaService.recalcularSlaPorMudancaPrioridade(ticket, TicketPriority.MEDIA);

        verify(historyService).recordSlaRecalculado(eq(ticket), eq(TicketPriority.MEDIA), eq(TicketPriority.ALTA), any());
    }

    @Test
    void recalcularSlaPorMudancaPrioridade_chamadoTerminal_deveIgnorar() {
        ticket.setStatus(TicketStatus.CANCELADO);

        slaService.recalcularSlaPorMudancaPrioridade(ticket, TicketPriority.MEDIA);

        verify(historyService, never()).recordSlaRecalculado(any(), any(), any(), any());
    }

    // ========== CONSULTA ==========

    @Test
    void consultarSla_comSucesso_deveRetornarDto() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.of(ticket));
        when(securityUtils.getCurrentUserOrThrow()).thenReturn(admin);

        var response = slaService.consultarSla(chamadoId);

        assertThat(response.chamadoId()).isEqualTo(chamadoId);
        assertThat(response.slaPrimeiraResposta()).isNotNull();
        assertThat(response.slaResolucao()).isNotNull();
    }

    @Test
    void consultarSla_chamadoNaoEncontrado_deveLancarExcecao() {
        when(ticketRepository.findById(chamadoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slaService.consultarSla(chamadoId))
                .isInstanceOf(TicketNotFoundException.class);
    }
}
