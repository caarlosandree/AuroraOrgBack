package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.SlaStatus;
import br.com.api.auroraorg.ticket.enums.SlaTipo;

import java.time.LocalDateTime;

/**
 * DTO de resumo de um SLA específico (primeira resposta ou resolução).
 */
public record SlaResumoResponse(
    SlaTipo tipo,
    String tipoLabel,
    SlaStatus status,
    String statusLabel,
    LocalDateTime prazo,
    LocalDateTime dataConclusao,
    Long tempoDecorridoSegundos,
    Long tempoRestanteSegundos,
    Long tempoExcedidoSegundos,
    Double percentualConsumido,
    Boolean emRisco
) {}
