package br.com.api.auroraorg.ticket.mapper;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.entity.Ticket;
import br.com.api.auroraorg.ticket.util.SlaCalculator;
import br.com.api.auroraorg.user.dto.UserResponse;
import br.com.api.auroraorg.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper para conversão entre Ticket (Entity) e DTOs.
 * 
 * Conforme regra backend-data.mdc, use MapStruct para mapeamentos
 * complexos. Este é um mapper manual simples para demonstração,
 * mas recomenda-se migrar para MapStruct em produção.
 */
@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final UserMapper userMapper;
    private final SlaCalculator slaCalculator;

    /**
     * Converte Ticket para TicketResponse (detalhado).
     */
    public TicketResponse toResponse(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        UserResponse requesterResponse = userMapper.toResponse(ticket.getRequester());
        UserResponse assigneeResponse = ticket.hasAssignee() 
            ? userMapper.toResponse(ticket.getAssignee()) 
            : null;

        return new TicketResponse(
            ticket.getId(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getStatus().getLabel(),
            ticket.getPriority(),
            ticket.getPriority().getLabel(),
            ticket.getCategory(),
            requesterResponse,
            assigneeResponse,
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getSlaDueAt(),
            ticket.getResolvedAt(),
            ticket.getRemainingSlaHours(),
            ticket.isSlaOverdue()
        );
    }

    /**
     * Converte Ticket para TicketSummaryResponse (resumido).
     */
    public TicketSummaryResponse toSummary(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new TicketSummaryResponse(
            ticket.getId(),
            ticket.getTitle(),
            ticket.getStatus(),
            ticket.getStatus().getLabel(),
            ticket.getPriority(),
            ticket.getPriority().getLabel(),
            ticket.getCategory(),
            Optional.ofNullable(ticket.getRequester()).map(r -> r.getName()).orElse(null),
            Optional.ofNullable(ticket.getAssignee()).map(a -> a.getName()).orElse(null),
            ticket.getCreatedAt(),
            ticket.getSlaDueAt(),
            ticket.isSlaOverdue()
        );
    }

    /**
     * Cria nova entidade a partir do CreateTicketRequest.
     * O solicitante e o SLA são definidos posteriormente no service.
     */
    public Ticket toEntity(CreateTicketRequest request) {
        if (request == null) {
            return null;
        }

        return Ticket.builder()
            .title(request.title())
            .description(request.description())
            .priority(request.priority())
            .category(request.category())
            .status(null)  // Será definido no @PrePersist como ABERTO
            .build();
    }

    /**
     * Atualiza entidade existente com dados do UpdateTicketRequest.
     */
    public void updateEntity(Ticket ticket, UpdateTicketRequest request) {
        if (ticket == null || request == null) {
            return;
        }

        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setCategory(request.category());
    }
}
