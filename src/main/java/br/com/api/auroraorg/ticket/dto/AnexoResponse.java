package br.com.api.auroraorg.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO resumido de anexo para listagem.
 * Não expõe storagePath.
 */
public record AnexoResponse(
        UUID id,
        UUID ticketId,
        String originalFileName,
        String contentType,
        long fileSize,
        UploadedByInfo uploadedBy,
        LocalDateTime createdAt
) {
    public record UploadedByInfo(UUID id, String name) {}
}
