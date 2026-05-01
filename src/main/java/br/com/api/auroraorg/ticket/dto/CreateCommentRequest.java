package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.CommentVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para criação de um novo comentário.
 *
 * Regras de validação:
 * - Conteúdo obrigatório (1-5000 caracteres)
 * - Visibilidade obrigatória
 *
 * Regras de negócio (validadas no service):
 * - SOLICITANTE só pode criar comentário PUBLICO
 * - AGENTE pode criar PUBLICO ou INTERNO
 * - Chamados FECHADOS/CANCELADOS não aceitam novos comentários
 */
public record CreateCommentRequest(

    @NotBlank(message = "O conteúdo do comentário é obrigatório")
    @Size(min = 1, max = 5000, message = "O comentário deve ter entre {min} e {max} caracteres")
    String content,

    @NotNull(message = "A visibilidade é obrigatória")
    CommentVisibility visibility
) {
}
