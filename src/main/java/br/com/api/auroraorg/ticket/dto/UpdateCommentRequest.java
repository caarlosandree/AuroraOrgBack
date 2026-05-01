package br.com.api.auroraorg.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização de um comentário existente.
 *
 * Regras:
 * - Apenas o autor ou ADMIN pode editar
 * - Comentários removidos não podem ser editados
 * - Apenas o conteúdo pode ser alterado (visibilidade não muda)
 */
public record UpdateCommentRequest(

    @NotBlank(message = "O conteúdo do comentário é obrigatório")
    @Size(min = 1, max = 5000, message = "O comentário deve ter entre {min} e {max} caracteres")
    String content
) {
}
