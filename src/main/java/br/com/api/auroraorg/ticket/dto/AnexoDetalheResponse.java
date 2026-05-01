package br.com.api.auroraorg.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO detalhado de anexo.
 * Inclui dados de remoção lógica para usuários com permissão administrativa.
 * Nunca expõe storagePath.
 */
public record AnexoDetalheResponse(
        UUID id,
        UUID ticketId,
        String originalFileName,
        String contentType,
        long fileSize,
        AnexoResponse.UploadedByInfo uploadedBy,
        LocalDateTime createdAt,
        boolean removed,
        LocalDateTime removedAt,
        RemovedByInfo removedBy
) {
    public record RemovedByInfo(UUID id, String name) {}
}
