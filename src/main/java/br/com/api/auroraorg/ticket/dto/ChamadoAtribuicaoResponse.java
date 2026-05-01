package br.com.api.auroraorg.ticket.dto;

import br.com.api.auroraorg.ticket.enums.TicketStatus;
import br.com.api.auroraorg.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resposta após operação de atribuição de chamado")
public record ChamadoAtribuicaoResponse(

    @Schema(description = "ID do chamado")
    UUID chamadoId,

    @Schema(description = "Status atual do chamado")
    TicketStatus status,

    @Schema(description = "Fila atual do chamado")
    FilaResponse fila,

    @Schema(description = "Categoria atual do chamado")
    CategoriaResponse categoria,

    @Schema(description = "Responsável atual do chamado")
    UserResponse responsavel,

    @Schema(description = "Mensagem descritiva da operação")
    String message
) {
}
