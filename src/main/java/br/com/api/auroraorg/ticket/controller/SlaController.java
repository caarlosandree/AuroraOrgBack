package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.service.SlaMonitoramentoService;
import br.com.api.auroraorg.ticket.service.SlaPoliticaService;
import br.com.api.auroraorg.ticket.service.SlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operações de SLA.
 *
 * Endpoints:
 * - GET /api/chamados/{chamadoId}/sla: consultar SLA do chamado
 * - POST /api/chamados/{chamadoId}/sla/recalcular: recalcular SLA manualmente (ADMIN)
 * - GET /api/sla/vencidos: listar chamados com SLA vencido
 * - GET /api/sla/em-risco: listar chamados em risco
 * - GET /api/sla/resumo: resumo de SLA para dashboard
 * - POST/GET/PUT/PATCH /api/sla/politicas: CRUD de políticas (ADMIN)
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "SLA", description = "Gerenciamento de Service Level Agreements")
public class SlaController {

    private final SlaService slaService;
    private final SlaMonitoramentoService slaMonitoramentoService;
    private final SlaPoliticaService slaPoliticaService;

    // ========== CONSULTA DE SLA DO CHAMADO ==========

    @Operation(summary = "Consultar SLA de um chamado",
            description = "Retorna o status completo do SLA de primeira resposta e resolução.")
    @GetMapping("/api/chamados/{chamadoId}/sla")
    @PreAuthorize("hasAnyRole('ADMIN','AGENTE','GESTOR','SOLICITANTE')")
    public ResponseEntity<ChamadoSlaResponse> consultarSla(@PathVariable UUID chamadoId) {
        return ResponseEntity.ok(slaService.consultarSla(chamadoId));
    }

    @Operation(summary = "Recalcular SLA manualmente",
            description = "Permite que ADMIN recalcule os prazos de SLA de um chamado.")
    @PostMapping("/api/chamados/{chamadoId}/sla/recalcular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChamadoSlaResponse> recalcularSla(
            @PathVariable UUID chamadoId,
            @Valid @RequestBody RecalcularSlaRequest request) {
        return ResponseEntity.ok(slaService.recalcularSla(chamadoId, request));
    }

    // ========== LISTAGENS DE MONITORAMENTO ==========

    @Operation(summary = "Listar chamados com SLA vencido",
            description = "Retorna resumo de chamados que ultrapassaram o prazo de resolução.")
    @GetMapping("/api/sla/vencidos")
    @PreAuthorize("hasAnyRole('ADMIN','AGENTE','GESTOR')")
    public ResponseEntity<List<ChamadoSlaResponse>> listarVencidos() {
        // Delega para monitoramento service, mas filtra por permissões no service
        var tickets = slaMonitoramentoService.buscarChamadosVencidos();
        // TODO: adicionar filtro por fila do agente quando necessário
        return ResponseEntity.ok(tickets.stream()
                .map(t -> slaService.consultarSla(t.getId()))
                .toList());
    }

    @Operation(summary = "Listar chamados com SLA em risco",
            description = "Retorna resumo de chamados próximos do vencimento (<= 20% do tempo restante).")
    @GetMapping("/api/sla/em-risco")
    @PreAuthorize("hasAnyRole('ADMIN','AGENTE','GESTOR')")
    public ResponseEntity<List<ChamadoSlaResponse>> listarEmRisco() {
        var tickets = slaMonitoramentoService.buscarChamadosEmRisco();
        return ResponseEntity.ok(tickets.stream()
                .map(t -> slaService.consultarSla(t.getId()))
                .toList());
    }

    @Operation(summary = "Resumo de SLA para dashboard",
            description = "Retorna métricas consolidadas de SLA do sistema.")
    @GetMapping("/api/sla/resumo")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<SlaDashboardResponse> resumo() {
        return ResponseEntity.ok(slaMonitoramentoService.gerarResumoDashboard());
    }

    // ========== CRUD DE POLÍTICAS DE SLA ==========

    @Operation(summary = "Criar política de SLA",
            description = "Cria uma nova política configurável de SLA. Apenas ADMIN.")
    @PostMapping("/api/sla/politicas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaPoliticaResponse> criarPolitica(
            @Valid @RequestBody CriarSlaPoliticaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaPoliticaService.criar(request));
    }

    @Operation(summary = "Listar políticas de SLA",
            description = "Retorna todas as políticas de SLA cadastradas.")
    @GetMapping("/api/sla/politicas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<List<SlaPoliticaResponse>> listarPoliticas() {
        return ResponseEntity.ok(slaPoliticaService.listar());
    }

    @Operation(summary = "Buscar política de SLA",
            description = "Retorna detalhes de uma política específica.")
    @GetMapping("/api/sla/politicas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<SlaPoliticaResponse> buscarPolitica(@PathVariable UUID id) {
        return ResponseEntity.ok(slaPoliticaService.buscar(id));
    }

    @Operation(summary = "Atualizar política de SLA",
            description = "Atualiza dados de uma política existente. Apenas ADMIN.")
    @PutMapping("/api/sla/politicas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlaPoliticaResponse> atualizarPolitica(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarSlaPoliticaRequest request) {
        return ResponseEntity.ok(slaPoliticaService.atualizar(id, request));
    }

    @Operation(summary = "Ativar política de SLA",
            description = "Ativa uma política de SLA. Apenas ADMIN.")
    @PatchMapping("/api/sla/politicas/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ativarPolitica(@PathVariable UUID id) {
        slaPoliticaService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desativar política de SLA",
            description = "Desativa uma política de SLA. Apenas ADMIN.")
    @PatchMapping("/api/sla/politicas/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativarPolitica(@PathVariable UUID id) {
        slaPoliticaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
