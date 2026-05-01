package br.com.api.auroraorg.ticket.dto;

import org.springframework.data.domain.Page;

/**
 * DTO wrapper para resposta paginada de chamados.
 * 
 * Inclui metadados de paginação junto com os dados.
 */
public record TicketListResponse(
    Page<TicketSummaryResponse> tickets,
    long totalElements,
    int totalPages,
    int currentPage,
    int pageSize,
    boolean hasNext,
    boolean hasPrevious
) {
    public TicketListResponse(Page<TicketSummaryResponse> page) {
        this(
            page,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.getSize(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
