package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.*;
import br.com.api.auroraorg.ticket.service.CategoriaService;
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
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categorias", description = "Gerenciamento de categorias de chamados")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    @Operation(summary = "Criar categoria", description = "Cria uma nova categoria de chamado. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CategoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Categoria com mesmo nome já existe")
    })
    public ResponseEntity<CategoriaResponse> criarCategoria(@Valid @RequestBody CriarCategoriaRequest request) {
        CategoriaResponse response = categoriaService.criarCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar categorias", description = "Lista todas as categorias. ADMIN vê todas, outros apenas ativas.")
    @ApiResponse(responseCode = "200", description = "Lista de categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias(
            @Parameter(description = "Filtrar apenas ativas")
            @RequestParam(defaultValue = "true") boolean apenasAtivas) {
        return ResponseEntity.ok(categoriaService.listarCategorias(apenasAtivas));
    }

    @GetMapping("/{categoriaId}")
    @Operation(summary = "Buscar categoria", description = "Retorna detalhes de uma categoria com métricas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    })
    public ResponseEntity<CategoriaDetalheResponse> buscarCategoria(
            @Parameter(description = "ID da categoria", required = true)
            @PathVariable UUID categoriaId) {
        return ResponseEntity.ok(categoriaService.buscarCategoriaDetalhe(categoriaId));
    }

    @PutMapping("/{categoriaId}")
    @Operation(summary = "Atualizar categoria", description = "Atualiza nome e descrição. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria atualizada"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada"),
            @ApiResponse(responseCode = "409", description = "Categoria com mesmo nome já existe")
    })
    public ResponseEntity<CategoriaResponse> atualizarCategoria(
            @PathVariable UUID categoriaId,
            @Valid @RequestBody AtualizarCategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.atualizarCategoria(categoriaId, request));
    }

    @PatchMapping("/{categoriaId}/ativar")
    @Operation(summary = "Ativar categoria", description = "Ativa uma categoria desativada. Apenas ADMIN.")
    public ResponseEntity<CategoriaResponse> ativarCategoria(@PathVariable UUID categoriaId) {
        return ResponseEntity.ok(categoriaService.ativarCategoria(categoriaId));
    }

    @PatchMapping("/{categoriaId}/desativar")
    @Operation(summary = "Desativar categoria", description = "Desativa uma categoria (não exclui). Apenas ADMIN.")
    public ResponseEntity<CategoriaResponse> desativarCategoria(@PathVariable UUID categoriaId) {
        return ResponseEntity.ok(categoriaService.desativarCategoria(categoriaId));
    }

    @PatchMapping("/{categoriaId}/fila-padrao")
    @Operation(summary = "Definir fila padrão", description = "Define a fila padrão para uma categoria. Apenas ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila padrão definida"),
            @ApiResponse(responseCode = "404", description = "Categoria ou fila não encontrada")
    })
    public ResponseEntity<CategoriaResponse> definirFilaPadrao(
            @PathVariable UUID categoriaId,
            @Valid @RequestBody DefinirFilaPadraoCategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.definirFilaPadrao(categoriaId, request));
    }
}
