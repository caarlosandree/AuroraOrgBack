package br.com.api.auroraorg.ticket.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Chave composta para a entidade FilaAgente.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilaAgenteId implements Serializable {

    private UUID filaId;
    private UUID agenteId;
}
