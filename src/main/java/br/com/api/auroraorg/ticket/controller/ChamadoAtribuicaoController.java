package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.service.ChamadoAtribuicaoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chamados")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Atribuição de Chamados", description = "Operações de fila, categoria e atribuição de chamados")
public class ChamadoAtribuicaoController {

    private final ChamadoAtribuicaoService atribuicaoService;

    @PatchMapping("/{chamadoId}/fila")
    @Operation(summary = "Transferir chamado de fila", description = "Transfere um chamado para outra fila. ADMIN ou GESTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila alterada",
                    content = @Content(schema = @Schema(implementation = ChamadoAtribuicaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Responsável não pertence à nova fila"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Chamado ou fila não encontrada")
    })
    public ResponseEntity<ChamadoAtribuicaoResponse> transferirFila(
            @PathVariable UUID chamadoId,
            @Valid @RequestBody TransferirFilaChamadoRequest request) {
        return ResponseEntity.ok(atribuicaoService.transferirFila(chamadoId, request));
    }

    @PatchMapping("/{chamadoId}/categoria")
    @Operation(summary = "Alterar categoria do chamado", description = "Altera a categoria de um chamado. ADMIN ou GESTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria alterada",
                    content = @Content(schema = @Schema(implementation = ChamadoAtribuicaoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Chamado ou categoria não encontrada")
    })
    public ResponseEntity<ChamadoAtribuicaoResponse> alterarCategoria(
            @PathVariable UUID chamadoId,
            @Valid @RequestBody AlterarCategoriaChamadoRequest request) {
        return ResponseEntity.ok(atribuicaoService.alterarCategoria(chamadoId, request));
    }

    @PatchMapping("/{chamadoId}/atribuir")
    @Operation(summary = "Atribuir responsável", description = "Atribui um responsável ao chamado. ADMIN ou GESTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsável atribuído",
                    content = @Content(schema = @Schema(implementation = ChamadoAtribuicaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Usuário não pode atender chamados ou não pertence à fila"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Chamado ou usuário não encontrado")
    })
    public ResponseEntity<ChamadoAtribuicaoResponse> atribuirResponsavel(
            @PathVariable UUID chamadoId,
            @Valid @RequestBody AtribuirResponsavelRequest request) {
        return ResponseEntity.ok(atribuicaoService.atribuirResponsavel(chamadoId, request));
    }

    @PatchMapping("/{chamadoId}/assumir")
    @Operation(summary = "Assumir chamado", description = "O usuário autenticado se torna responsável. AGENTE, ADMIN ou GESTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chamado assumido",
                    content = @Content(schema = @Schema(implementation = ChamadoAtribuicaoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem permissão ou agente não pertence à fila"),
            @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<ChamadoAtribuicaoResponse> assumirChamado(
            @Parameter(description = "ID do chamado", required = true)
            @PathVariable UUID chamadoId) {
        return ResponseEntity.ok(atribuicaoService.assumirChamado(chamadoId));
    }

    @PatchMapping("/{chamadoId}/remover-responsavel")
    @Operation(summary = "Remover responsável", description = "Remove o responsável do chamado. ADMIN ou GESTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Responsável removido",
                    content = @Content(schema = @Schema(implementation = ChamadoAtribuicaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Chamado não possui responsável"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "404", description = "Chamado não encontrado")
    })
    public ResponseEntity<ChamadoAtribuicaoResponse> removerResponsavel(
            @PathVariable UUID chamadoId) {
        return ResponseEntity.ok(atribuicaoService.removerResponsavel(chamadoId));
    }
}
