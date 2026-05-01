package br.com.api.auroraorg.ticket.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitar recálculo manual do SLA de um chamado.
 */
public record RecalcularSlaRequest(
    @NotNull(message = "A flag manterSLAOriginal é obrigatória")
    Boolean manterSlaOriginal,

    String motivo
) {}
