package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para criação de um novo chamado.
 * 
 * Regras:
 * - Título obrigatório (1-200 caracteres)
 * - Descrição obrigatória (mínimo 10 caracteres)
 * - Prioridade opcional (padrão: MEDIA)
 * - Categoria opcional
 * - Solicitante preenchido automaticamente pelo sistema
 */
public record CreateTicketRequest(
    
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 1, max = 200, message = "O título deve ter entre {min} e {max} caracteres")
    String title,
    
    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 10, max = 5000, message = "A descrição deve ter entre {min} e {max} caracteres")
    String description,
    
    TicketPriority priority,

    @NotNull(message = "A categoria é obrigatória")
    @Schema(description = "ID da categoria do chamado", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    java.util.UUID categoriaId,

    @Size(max = 100, message = "A categoria deve ter no máximo {max} caracteres")
    String category
) {
    /**
     * Retorna a prioridade informada ou MEDIA como padrão.
     */
    public TicketPriority priority() {
        return priority != null ? priority : TicketPriority.MEDIA;
    }
}
