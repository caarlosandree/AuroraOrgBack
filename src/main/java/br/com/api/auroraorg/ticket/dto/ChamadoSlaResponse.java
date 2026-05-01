package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.dto.UserResponse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta completa do SLA de um chamado.
 */
public record ChamadoSlaResponse(
    UUID chamadoId,
    String chamadoTitulo,
    TicketStatus status,
    TicketPriority prioridade,
    SlaResumoResponse slaPrimeiraResposta,
    SlaResumoResponse slaResolucao,
    UserResponse primeiraRespostaPor,
    Boolean chamadoVencido,
    Boolean chamadoEmRisco,
    LocalDateTime criadoEm
) {}
