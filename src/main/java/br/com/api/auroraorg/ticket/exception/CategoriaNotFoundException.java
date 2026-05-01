package br.com.api.auroraorg.ticket.exception;

import java.util.UUID;

public class CategoriaNotFoundException extends RuntimeException {

    public CategoriaNotFoundException(UUID categoriaId) {
        super(String.format("Categoria não encontrada com ID: %s", categoriaId));
    }
}
