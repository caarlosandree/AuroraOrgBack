package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Visibilidade de comentários e eventos na timeline.
 *
 * Regras de acesso:
 * - PUBLICO: visível para SOLICITANTE, AGENTE, GESTOR e ADMIN
 * - INTERNO: visível apenas para ADMIN, AGENTE e GESTOR
 *
 * O perfil GESTOR pode visualizar conteúdo INTERNO mas normalmente não deve criar comentários,
 * salvo regra explícita do sistema.
 */
@Getter
public enum CommentVisibility {

    PUBLICO("Público", "Visível para todos os usuários relacionados ao chamado"),
    INTERNO("Interno", "Visível apenas para equipe de atendimento (ADMIN, AGENTE, GESTOR)");

    private final String label;
    private final String description;

    CommentVisibility(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * Verifica se a visibilidade é pública.
     */
    public boolean isPublic() {
        return this == PUBLICO;
    }

    /**
     * Verifica se a visibilidade é interna.
     */
    public boolean isInternal() {
        return this == INTERNO;
    }
}
