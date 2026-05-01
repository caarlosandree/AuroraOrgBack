package br.com.api.auroraorg.ticket.config;

import br.com.api.auroraorg.ticket.service.SlaMonitoramentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de monitoramento automático de SLA.
 *
 * Executa periodicamente para:
 * - Verificar chamados em risco
 * - Verificar chamados vencidos
 * - Atualizar status de SLA
 * - Registrar eventos de histórico
 *
 * Configuração via application.yml:
 * sla.monitoramento.enabled: true/false
 * sla.monitoramento.intervalo-ms: 300000 (5 minutos)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sla.monitoramento.enabled", havingValue = "true", matchIfMissing = true)
public class SlaMonitoramentoScheduler {

    private final SlaMonitoramentoService monitoramentoService;

    /**
     * Executa monitoramento a cada intervalo configurado.
     * Padrão: a cada 5 minutos (300.000 ms).
     */
    @Scheduled(fixedRateString = "${sla.monitoramento.intervalo-ms:300000}")
    public void executarMonitoramento() {
        log.debug("Scheduler de monitoramento de SLA iniciado");
        monitoramentoService.executarMonitoramento();
        log.debug("Scheduler de monitoramento de SLA finalizado");
    }
}
