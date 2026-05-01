package br.com.api.auroraorg.ticket.controller;

import br.com.api.auroraorg.ticket.dto.AnexoDetalheResponse;
import br.com.api.auroraorg.ticket.dto.AnexoResponse;
import br.com.api.auroraorg.ticket.dto.UploadAnexoResponse;
import br.com.api.auroraorg.ticket.service.ChamadoAnexoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para o Módulo 4 — Anexos de Chamados.
 *
 * Endpoints:
 * - POST   /api/v1/chamados/{chamadoId}/anexos           — upload
 * - GET    /api/v1/chamados/{chamadoId}/anexos           — listagem
 * - GET    /api/v1/chamados/{chamadoId}/anexos/{id}      — detalhe
 * - GET    /api/v1/chamados/{chamadoId}/anexos/{id}/download — download
 * - DELETE /api/v1/chamados/{chamadoId}/anexos/{id}      — remoção lógica
 *
 * O caminho físico do arquivo (storagePath) NUNCA é exposto na API.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chamados/{chamadoId}/anexos")
@RequiredArgsConstructor
@Tag(name = "Anexos", description = "Gerenciamento de anexos de chamados")
public class ChamadoAnexoController {

    private final ChamadoAnexoService anexoService;

    @Operation(
        summary = "Upload de anexo",
        description = "Envia um arquivo como anexo ao chamado. Content-Type: multipart/form-data.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Arquivo enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido, vazio ou tipo não permitido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permissão negada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chamado não encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Limite de anexos excedido ou chamado não permite anexo", content = @Content)
        }
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadAnexoResponse> upload(
            @Parameter(description = "ID do chamado") @PathVariable UUID chamadoId,
            @RequestParam("arquivo") MultipartFile arquivo) {

        log.info("Upload de anexo no chamado {}. Tamanho: {} bytes", chamadoId,
                arquivo != null ? arquivo.getSize() : "null");

        UploadAnexoResponse response = anexoService.upload(chamadoId, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Listar anexos",
        description = "Lista todos os anexos ativos do chamado. SOLICITANTE vê apenas do próprio chamado.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Lista de anexos"),
            @ApiResponse(responseCode = "403", description = "Permissão negada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chamado não encontrado", content = @Content)
        }
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AnexoResponse>> listar(
            @Parameter(description = "ID do chamado") @PathVariable UUID chamadoId) {

        List<AnexoResponse> anexos = anexoService.listar(chamadoId);
        return ResponseEntity.ok(anexos);
    }

    @Operation(
        summary = "Detalhe de anexo",
        description = "Retorna os metadados detalhados de um anexo específico.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Detalhe do anexo"),
            @ApiResponse(responseCode = "403", description = "Permissão negada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anexo ou chamado não encontrado", content = @Content)
        }
    )
    @GetMapping("/{anexoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnexoDetalheResponse> detalhe(
            @Parameter(description = "ID do chamado") @PathVariable UUID chamadoId,
            @Parameter(description = "ID do anexo") @PathVariable UUID anexoId) {

        AnexoDetalheResponse detalhe = anexoService.detalhe(chamadoId, anexoId);
        return ResponseEntity.ok(detalhe);
    }

    @Operation(
        summary = "Download de anexo",
        description = "Faz o download do arquivo. Usa o nome original no header Content-Disposition. " +
                      "O caminho físico de armazenamento NUNCA é exposto.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Arquivo para download"),
            @ApiResponse(responseCode = "403", description = "Permissão negada ou anexo removido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anexo ou chamado não encontrado", content = @Content)
        }
    )
    @GetMapping("/{anexoId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(
            @Parameter(description = "ID do chamado") @PathVariable UUID chamadoId,
            @Parameter(description = "ID do anexo") @PathVariable UUID anexoId) {

        Resource resource = anexoService.download(chamadoId, anexoId);
        String nomeOriginal = anexoService.nomeOriginalParaDownload(chamadoId, anexoId);

        String nomeEncoded = URLEncoder.encode(nomeOriginal, StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentType = determinarContentType(nomeOriginal);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomeOriginal + "\"; filename*=UTF-8''" + nomeEncoded)
                .body(resource);
    }

    @Operation(
        summary = "Remover anexo",
        description = "Remove logicamente o anexo. O arquivo físico é preservado para auditoria.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Anexo removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Permissão negada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Anexo ou chamado não encontrado", content = @Content)
        }
    )
    @DeleteMapping("/{anexoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do chamado") @PathVariable UUID chamadoId,
            @Parameter(description = "ID do anexo") @PathVariable UUID anexoId) {

        anexoService.remover(chamadoId, anexoId);
        return ResponseEntity.noContent().build();
    }

    // ========== AUXILIAR ==========

    private String determinarContentType(String nomeOriginal) {
        String ext = nomeOriginal.contains(".")
                ? nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase()
                : "";
        return switch (ext) {
            case "pdf"  -> "application/pdf";
            case "png"  -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "txt"  -> "text/plain";
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls"  -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default     -> "application/octet-stream";
        };
    }
}
