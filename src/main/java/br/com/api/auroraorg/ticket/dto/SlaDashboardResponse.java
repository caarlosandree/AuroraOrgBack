package br.com.api.auroraorg.ticket.dto;

import java.util.Map;

/**
 * DTO de resumo de SLA para dashboard e métricas.
 */
public record SlaDashboardResponse(
    long totalChamados,
    long chamadosDentroDoPrazo,
    long chamadosEmRisco,
    long chamadosVencidos,
    long chamadosCumpridos,
    long chamadosViolados,
    long chamadosCancelados,
    Double percentualCumprimento,
    Double percentualViolacao,
    Map<String, Long> chamadosPorPrioridade,
    Map<String, Long> chamadosVencidosPorPrioridade
) {}
