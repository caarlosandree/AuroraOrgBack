package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class AgenteNaoPermitidoException extends RuntimeException {

    public AgenteNaoPermitidoException(UUID userId) {
        super(String.format("Usuário %s não possui perfil de AGENTE e não pode ser vinculado a uma fila", userId));
    }
}
