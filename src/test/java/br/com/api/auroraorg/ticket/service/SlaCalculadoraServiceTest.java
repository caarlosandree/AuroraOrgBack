package br.com.api.auroraorg.ticket.service;

import br.com.api.auroraorg.ticket.entity.SlaPolitica;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.repository.SlaPoliticaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaCalculadoraServiceTest {

    @Mock
    private SlaPoliticaRepository slaPoliticaRepository;

    @InjectMocks
    private SlaCalculadoraService slaCalculadora;

    private LocalDateTime agora;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.of(2026, 1, 15, 10, 0);
        ticket = Ticket.builder()
                .priority(TicketPriority.MEDIA)
                .createdAt(agora)
                .build();
    }

    // ========== CÁLCULO DE PRAZOS ==========

    @Test
    void calcularPrazoPrimeiraResposta_BAIXA_deveRetornar24Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoPrimeiraResposta(TicketPriority.BAIXA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(24));
    }

    @Test
    void calcularPrazoPrimeiraResposta_MEDIA_deveRetornar12Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoPrimeiraResposta(TicketPriority.MEDIA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(12));
    }

    @Test
    void calcularPrazoPrimeiraResposta_ALTA_deveRetornar4Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoPrimeiraResposta(TicketPriority.ALTA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(4));
    }

    @Test
    void calcularPrazoPrimeiraResposta_CRITICA_deveRetornar1Hora() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoPrimeiraResposta(TicketPriority.CRITICA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(1));
    }

    @Test
    void calcularPrazoResolucao_BAIXA_deveRetornar72Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoResolucao(TicketPriority.BAIXA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(72));
    }

    @Test
    void calcularPrazoResolucao_MEDIA_deveRetornar48Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoResolucao(TicketPriority.MEDIA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(48));
    }

    @Test
    void calcularPrazoResolucao_ALTA_deveRetornar24Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoResolucao(TicketPriority.ALTA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(24));
    }

    @Test
    void calcularPrazoResolucao_CRITICA_deveRetornar4Horas() {
        LocalDateTime prazo = slaCalculadora.calcularPrazoResolucao(TicketPriority.CRITICA, agora);
        assertThat(prazo).isEqualTo(agora.plusHours(4));
    }

    // ========== STATUS DE PRIMEIRA RESPOSTA ==========

    @Test
    void calcularStatusPrimeiraResposta_dentroDoPrazo_deveRetornarCUMPRIDO() {
        LocalDateTime prazo = agora.plusHours(12);
        LocalDateTime resposta = agora.plusHours(5);
        SlaStatus status = slaCalculadora.calcularStatusPrimeiraResposta(resposta, prazo);
        assertThat(status).isEqualTo(SlaStatus.CUMPRIDO);
    }

    @Test
    void calcularStatusPrimeiraResposta_foraDoPrazo_deveRetornarVIOLADO() {
        LocalDateTime prazo = agora.plusHours(12);
        LocalDateTime resposta = agora.plusHours(15);
        SlaStatus status = slaCalculadora.calcularStatusPrimeiraResposta(resposta, prazo);
        assertThat(status).isEqualTo(SlaStatus.VIOLADO);
    }

    @Test
    void calcularStatusPrimeiraResposta_semRespostaAindaDentroDoPrazo_deveRetornarDENTRO_DO_PRAZO() {
        LocalDateTime prazo = LocalDateTime.now().plusHours(12);
        SlaStatus status = slaCalculadora.calcularStatusPrimeiraResposta(null, prazo);
        assertThat(status).isEqualTo(SlaStatus.DENTRO_DO_PRAZO);
    }

    @Test
    void calcularStatusPrimeiraResposta_semRespostaAtrasado_deveRetornarVENCIDO() {
        LocalDateTime prazo = LocalDateTime.now().minusHours(1);
        SlaStatus status = slaCalculadora.calcularStatusPrimeiraResposta(null, prazo);
        assertThat(status).isEqualTo(SlaStatus.VENCIDO);
    }

    // ========== STATUS DE RESOLUÇÃO ==========

    @Test
    void calcularStatusResolucao_dentroDoPrazo_deveRetornarCUMPRIDO() {
        LocalDateTime prazo = agora.plusHours(48);
        LocalDateTime resolucao = agora.plusHours(20);
        SlaStatus status = slaCalculadora.calcularStatusResolucao(resolucao, prazo);
        assertThat(status).isEqualTo(SlaStatus.CUMPRIDO);
    }

    @Test
    void calcularStatusResolucao_foraDoPrazo_deveRetornarVIOLADO() {
        LocalDateTime prazo = agora.plusHours(48);
        LocalDateTime resolucao = agora.plusHours(50);
        SlaStatus status = slaCalculadora.calcularStatusResolucao(resolucao, prazo);
        assertThat(status).isEqualTo(SlaStatus.VIOLADO);
    }

    @Test
    void calcularStatusResolucao_semResolucaoAtrasado_deveRetornarVENCIDO() {
        LocalDateTime prazo = agora.minusHours(2);
        SlaStatus status = slaCalculadora.calcularStatusResolucao(null, prazo);
        assertThat(status).isEqualTo(SlaStatus.VENCIDO);
    }

    // ========== RISCO ==========

    @Test
    void isEmRiscoPadrao_restando19PorCento_deveRetornarTrue() {
        LocalDateTime criadoEm = agora.minusHours(40);  // 40h de 48h = ~83%
        LocalDateTime prazo = agora.plusHours(8);      // 8h restantes = ~17%
        assertThat(slaCalculadora.isEmRiscoPadrao(criadoEm, prazo, agora)).isTrue();
    }

    @Test
    void isEmRiscoPadrao_restando25PorCento_deveRetornarFalse() {
        LocalDateTime criadoEm = agora.minusHours(36);  // 36h de 48h = 75%
        LocalDateTime prazo = agora.plusHours(12);     // 12h restantes = 25%
        assertThat(slaCalculadora.isEmRiscoPadrao(criadoEm, prazo, agora)).isFalse();
    }

    @Test
    void isEmRiscoPadrao_jaVencido_deveRetornarFalse() {
        LocalDateTime criadoEm = agora.minusHours(50);
        LocalDateTime prazo = agora.minusHours(2);
        assertThat(slaCalculadora.isEmRiscoPadrao(criadoEm, prazo, agora)).isFalse();
    }

    // ========== TEMPOS ==========

    @Test
    void calcularTempoDecorridoSegundos_deveCalcularCorretamente() {
        long segundos = slaCalculadora.calcularTempoDecorridoSegundos(agora, agora.plusHours(5));
        assertThat(segundos).isEqualTo(5 * 3600);
    }

    @Test
    void calcularTempoRestanteSegundos_dentroDoPrazo_deveRetornarPositivo() {
        long segundos = slaCalculadora.calcularTempoRestanteSegundos(agora.plusHours(10), agora);
        assertThat(segundos).isEqualTo(10 * 3600);
    }

    @Test
    void calcularTempoRestanteSegundos_vencido_deveRetornarZero() {
        long segundos = slaCalculadora.calcularTempoRestanteSegundos(agora.minusHours(2), agora);
        assertThat(segundos).isEqualTo(0);
    }

    @Test
    void calcularTempoExcedidoSegundos_vencido_deveRetornarPositivo() {
        long segundos = slaCalculadora.calcularTempoExcedidoSegundos(agora.minusHours(3), agora);
        assertThat(segundos).isEqualTo(3 * 3600);
    }

    @Test
    void calcularTempoExcedidoSegundos_dentroDoPrazo_deveRetornarZero() {
        long segundos = slaCalculadora.calcularTempoExcedidoSegundos(agora.plusHours(5), agora);
        assertThat(segundos).isEqualTo(0);
    }

    // ========== PERCENTUAL CONSUMIDO ==========

    @Test
    void calcularPercentualConsumido_50PorCento() {
        LocalDateTime criadoEm = agora.minusHours(24);
        LocalDateTime prazo = agora.plusHours(24);
        double pct = slaCalculadora.calcularPercentualConsumido(criadoEm, prazo, agora);
        assertThat(pct).isEqualTo(50.0);
    }

    @Test
    void calcularPercentualConsumido_naoUltrapassa100() {
        LocalDateTime criadoEm = agora.minusHours(100);
        LocalDateTime prazo = agora.minusHours(10);
        double pct = slaCalculadora.calcularPercentualConsumido(criadoEm, prazo, agora);
        assertThat(pct).isEqualTo(100.0);
    }

    // ========== RECÁLCULO ==========

    @Test
    void recalcularPrazos_mudarParaCRITICA_deveReduzirPrazos() {
        ticket.setPrazoPrimeiraResposta(agora.plusHours(12));
        ticket.setPrazoResolucao(agora.plusHours(48));
        ticket.setPriority(TicketPriority.CRITICA);

        slaCalculadora.recalcularPrazos(ticket, TicketPriority.CRITICA);

        assertThat(ticket.getPrazoPrimeiraResposta()).isEqualTo(agora.plusHours(1));
        assertThat(ticket.getPrazoResolucao()).isEqualTo(agora.plusHours(4));
    }

    // ========== INICIALIZAÇÃO ==========

    @Test
    void inicializarSla_devePreencherTodosOsCampos() {
        Ticket novo = Ticket.builder()
                .priority(TicketPriority.ALTA)
                .createdAt(agora)
                .build();

        slaCalculadora.inicializarSla(novo);

        assertThat(novo.getPrazoPrimeiraResposta()).isEqualTo(agora.plusHours(4));
        assertThat(novo.getPrazoResolucao()).isEqualTo(agora.plusHours(24));
        assertThat(novo.getSlaPrimeiraRespostaStatus()).isEqualTo(SlaStatus.DENTRO_DO_PRAZO);
        assertThat(novo.getSlaResolucaoStatus()).isEqualTo(SlaStatus.DENTRO_DO_PRAZO);
    }

    // ========== POLÍTICAS ==========

    @Test
    void resolverPolitica_comPoliticaAtiva_deveRetornarPolitica() {
        SlaPolitica politica = SlaPolitica.builder()
                .nome("Padrão MEDIA")
                .prioridade(TicketPriority.MEDIA)
                .prazoPrimeiraRespostaMinutos(12 * 60)
                .prazoResolucaoMinutos(48 * 60)
                .ativa(true)
                .build();

        when(slaPoliticaRepository.findByAtivaTrueAndPrioridadeAndFilaIsNullAndCategoriaIsNull(TicketPriority.MEDIA))
                .thenReturn(Optional.of(politica));

        SlaPolitica resultado = slaCalculadora.resolverPolitica(ticket);

        assertThat(resultado).isEqualTo(politica);
    }

    @Test
    void resolverPolitica_semPolitica_deveRetornarPoliticaPadrao() {
        when(slaPoliticaRepository.findByAtivaTrueAndPrioridadeAndFilaIsNullAndCategoriaIsNull(TicketPriority.MEDIA))
                .thenReturn(Optional.empty());

        SlaPolitica resultado = slaCalculadora.resolverPolitica(ticket);

        assertThat(resultado.getPrazoPrimeiraRespostaMinutos()).isEqualTo(12 * 60);
        assertThat(resultado.getPrazoResolucaoMinutos()).isEqualTo(48 * 60);
    }
}
