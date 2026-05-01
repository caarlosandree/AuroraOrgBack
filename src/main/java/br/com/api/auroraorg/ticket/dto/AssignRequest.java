package br.com.api.auroraorg.ticket.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para atribuição de responsável a um chamado.
 * 
 * Apenas ADMIN ou GESTOR podem atribuir um chamado a outro usuário.
 * O responsável deve ter perfil AGENTE ou ADMIN.
 */
public record AssignRequest(
    
    @NotNull(message = "O ID do responsável é obrigatório")
    UUID assigneeId
) {}
