package br.com.api.auroraorg.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para upload bem-sucedido de anexo.
 * Nunca expõe storagePath.
 */
public record UploadAnexoResponse(
        UUID id,
        UUID ticketId,
        String originalFileName,
        String contentType,
        long fileSize,
        LocalDateTime createdAt,
        String message
) {}
