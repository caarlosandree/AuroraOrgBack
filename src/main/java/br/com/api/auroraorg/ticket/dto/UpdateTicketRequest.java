package br.com.api.auroraorg.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização dos dados básicos de um chamado.
 * 
 * Permite alterar:
 * - Título
 * - Descrição
 * - Categoria
 * 
 * NÃO permite alterar:
 * - Status (use UpdateStatusRequest)
 * - Prioridade (regras específicas)
 * - Responsável (use AssignRequest)
 */
public record UpdateTicketRequest(
    
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 1, max = 200, message = "O título deve ter entre {min} e {max} caracteres")
    String title,
    
    @NotBlank(message = "A descrição é obrigatória")
    @Size(min = 10, max = 5000, message = "A descrição deve ter entre {min} e {max} caracteres")
    String description,
    
    @Size(max = 100, message = "A categoria deve ter no máximo {max} caracteres")
    String category
) {}
