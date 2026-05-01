package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.service.FilaAgenteService;
import br.com.api.auroraorg.ticket.service.FilaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/filas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Filas", description = "Gerenciamento de filas de atendimento")
public class FilaController {

    private final FilaService filaService;
    private final FilaAgenteService filaAgenteService;

    @PostMapping
    @Operation(summary = "Criar fila", description = "Cria uma nova fila de atendimento. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fila criada com sucesso",
                    content = @Content(schema = @Schema(implementation = FilaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Fila com mesmo nome já existe")
    })
    public ResponseEntity<FilaResponse> criarFila(@Valid @RequestBody CriarFilaRequest request) {
        FilaResponse response = filaService.criarFila(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar filas", description = "Lista todas as filas. ADMIN vê todas, outros apenas ativas.")
    @ApiResponse(responseCode = "200", description = "Lista de filas")
    public ResponseEntity<List<FilaResponse>> listarFilas(
            @Parameter(description = "Filtrar apenas ativas")
            @RequestParam(defaultValue = "true") boolean apenasAtivas) {
        return ResponseEntity.ok(filaService.listarFilas(apenasAtivas));
    }

    @GetMapping("/{filaId}")
    @Operation(summary = "Buscar fila", description = "Retorna detalhes de uma fila com agentes vinculados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila encontrada"),
            @ApiResponse(responseCode = "404", description = "Fila não encontrada")
    })
    public ResponseEntity<FilaDetalheResponse> buscarFila(
            @Parameter(description = "ID da fila", required = true)
            @PathVariable UUID filaId) {
        return ResponseEntity.ok(filaService.buscarFilaComAgentes(filaId));
    }

    @PutMapping("/{filaId}")
    @Operation(summary = "Atualizar fila", description = "Atualiza nome e descrição da fila. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila atualizada"),
            @ApiResponse(responseCode = "404", description = "Fila não encontrada"),
            @ApiResponse(responseCode = "409", description = "Fila com mesmo nome já existe")
    })
    public ResponseEntity<FilaResponse> atualizarFila(
            @PathVariable UUID filaId,
            @Valid @RequestBody AtualizarFilaRequest request) {
        return ResponseEntity.ok(filaService.atualizarFila(filaId, request));
    }

    @PatchMapping("/{filaId}/ativar")
    @Operation(summary = "Ativar fila", description = "Ativa uma fila desativada. Apenas ADMIN.")
    public ResponseEntity<FilaResponse> ativarFila(@PathVariable UUID filaId) {
        return ResponseEntity.ok(filaService.ativarFila(filaId));
    }

    @PatchMapping("/{filaId}/desativar")
    @Operation(summary = "Desativar fila", description = "Desativa uma fila (não exclui). Apenas ADMIN.")
    public ResponseEntity<FilaResponse> desativarFila(@PathVariable UUID filaId) {
        return ResponseEntity.ok(filaService.desativarFila(filaId));
    }

    @PostMapping("/{filaId}/agentes/{agenteId}")
    @Operation(summary = "Adicionar agente à fila", description = "Vincula um agente a uma fila. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agente adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Usuário não é agente ou já vinculado"),
            @ApiResponse(responseCode = "404", description = "Fila ou usuário não encontrado")
    })
    public ResponseEntity<Void> adicionarAgente(
            @PathVariable UUID filaId,
            @PathVariable UUID agenteId) {
        filaAgenteService.adicionarAgente(filaId, agenteId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{filaId}/agentes/{agenteId}")
    @Operation(summary = "Remover agente da fila", description = "Desvincula um agente de uma fila. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agente removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Agente não vinculado à fila"),
            @ApiResponse(responseCode = "404", description = "Fila não encontrada")
    })
    public ResponseEntity<Void> removerAgente(
            @PathVariable UUID filaId,
            @PathVariable UUID agenteId) {
        filaAgenteService.removerAgente(filaId, agenteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{filaId}/agentes")
    @Operation(summary = "Listar agentes da fila", description = "Retorna todos os agentes vinculados à fila")
    public ResponseEntity<List<FilaAgenteResponse>> listarAgentesDaFila(@PathVariable UUID filaId) {
        return ResponseEntity.ok(filaAgenteService.listarAgentesDaFila(filaId));
    }
}
