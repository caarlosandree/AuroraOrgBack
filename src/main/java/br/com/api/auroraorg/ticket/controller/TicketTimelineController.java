package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.TicketEventResponse;
import br.com.api.auroraorg.ticket.dto.TimelineItemResponse;
import br.com.api.auroraorg.ticket.service.TicketHistoryService;
import br.com.api.auroraorg.ticket.service.TicketTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para Timeline e Histórico de Chamados.
 *
 * Base path: /api/tickets/{ticketId}
 * Todas as operações requerem autenticação (JWT).
 *
 * Endpoints:
 * - GET /timeline: Timeline unificada (comentários + eventos)
 * - GET /history: Eventos de histórico (auditáveis)
 */
@RestController
@RequestMapping("/api/tickets/{ticketId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Timeline e Histórico", description = "Visualização de timeline e eventos de histórico")
public class TicketTimelineController {

    private final TicketTimelineService timelineService;
    private final TicketHistoryService historyService;

    /**
     * GET /api/tickets/{ticketId}/timeline
     * Retorna a timeline unificada do chamado.
     *
     * A timeline combina comentários e eventos de histórico em ordem cronológica.
     * O filtro de visibilidade é aplicado conforme o perfil do usuário:
     * - SOLICITANTE: apenas itens públicos
     * - AGENTE/ADMIN/GESTOR: itens públicos e internos
     */
    @GetMapping("/timeline")
    @Operation(summary = "Timeline do chamado",
               description = "Retorna timeline unificada com comentários e eventos em ordem cronológica")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Timeline retornada com sucesso",
                     content = @Content(schema = @Schema(implementation = TimelineItemResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<List<TimelineItemResponse>> getTimeline(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(timelineService.getTimeline(ticketId, pageable).getContent());
    }

    /**
     * GET /api/tickets/{ticketId}/timeline/recent
     * Retorna os últimos itens da timeline (mais recentes primeiro).
     * Útil para notificações e "últimas atividades".
     */
    @GetMapping("/timeline/recent")
    @Operation(summary = "Timeline recente",
               description = "Retorna os últimos N itens da timeline (mais recentes primeiro)")
    @ApiResponse(responseCode = "200", description = "Timeline recente retornada")
    public ResponseEntity<List<TimelineItemResponse>> getRecentTimeline(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "Quantidade de itens")
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(timelineService.getRecentTimelineItems(ticketId, count));
    }

    /**
     * GET /api/tickets/{ticketId}/history
     * Retorna os eventos de histórico (auditáveis) do chamado.
     *
     * Diferença entre timeline e history:
     * - Timeline: mistura comentários e eventos (visualização usuário)
     * - History: apenas eventos (auditoria, relatórios)
     */
    @GetMapping("/history")
    @Operation(summary = "Histórico do chamado",
               description = "Retorna apenas eventos de histórico (sem comentários)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico retornado",
                     content = @Content(schema = @Schema(implementation = TicketEventResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar"),
        @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<List<TicketEventResponse>> getHistory(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId,
            @Parameter(description = "Número da página (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(historyService.listAllVisibleEvents(ticketId, pageable).getContent());
    }

    /**
     * GET /api/tickets/{ticketId}/history/all
     * Retorna todos os eventos (acesso administrativo/auditoria).
     * Inclui eventos internos que podem não aparecer na timeline normal.
     */
    @GetMapping("/history/all")
    @Operation(summary = "Histórico completo (ADMIN)",
               description = "Retorna todos os eventos para auditoria (inclui internos)")
    @ApiResponse(responseCode = "200", description = "Histórico completo retornado")
    public ResponseEntity<List<TicketEventResponse>> getFullHistory(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID ticketId) {
        return ResponseEntity.ok(historyService.listAllEvents(ticketId));
    }
}
