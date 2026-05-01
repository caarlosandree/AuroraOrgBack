package br.com.api.auroraorg.ticket.enums;

import lombok.Getter;

/**
 * Tipos de eventos registrados no histórico de chamados.
 *
 * Eventos são imutáveis e representam ações importantes no ciclo de vida do chamado.
 * São usados para construir a timeline e auditoria.
 */
@Getter
public enum TicketEventType {

    // Eventos de ciclo de vida
    CHAMADO_CRIADO("Chamado criado", "Criação do chamado no sistema"),
    CHAMADO_RESOLVIDO("Chamado resolvido", "Chamado marcado como resolvido"),
    CHAMADO_FECHADO("Chamado fechado", "Chamado encerrado definitivamente"),
    CHAMADO_CANCELADO("Chamado cancelado", "Chamado cancelado"),

    // Eventos de comentários
    COMENTARIO_ADICIONADO("Comentário adicionado", "Novo comentário no chamado"),
    COMENTARIO_EDITADO("Comentário editado", "Comentário foi modificado"),
    COMENTARIO_REMOVIDO("Comentário removido", "Comentário removido logicamente"),

    // Eventos de alterações
    STATUS_ALTERADO("Status alterado", "Mudança no status do chamado"),
    PRIORIDADE_ALTERADA("Prioridade alterada", "Mudança na prioridade do chamado"),
    CAMPO_ALTERADO("Campo alterado", "Alteração em campo do chamado"),

    // Eventos de atribuição
    RESPONSAVEL_ATRIBUIDO("Responsável atribuído", "Primeira atribuição de responsável"),
    RESPONSAVEL_ALTERADO("Responsável alterado", "Troca de responsável"),
    RESPONSAVEL_REMOVIDO("Responsável removido", "Responsável removido do chamado"),

    // Eventos de anexos
    ANEXO_ADICIONADO("Anexo adicionado", "Arquivo anexado ao chamado"),
    ANEXO_REMOVIDO("Anexo removido", "Anexo removido logicamente do chamado"),

    // Eventos de fila e categoria
    FILA_ALTERADA("Fila alterada", "Chamado transferido para outra fila"),
    CATEGORIA_ALTERADA("Categoria alterada", "Categoria do chamado foi alterada"),

    // Eventos de agente em fila
    AGENTE_ADICIONADO_FILA("Agente adicionado à fila", "Novo agente vinculado à fila"),
    AGENTE_REMOVIDO_FILA("Agente removido da fila", "Agente desvinculado da fila"),

    // Eventos de assumir chamado
    CHAMADO_ASSUMIDO("Chamado assumido", "Agente assumiu o chamado"),

    // Eventos de SLA
    SLA_EM_RISCO("SLA em risco", "SLA próximo do vencimento"),
    SLA_VENCIDO("SLA vencido", "SLA foi violado"),
    SLA_PRIMEIRA_RESPOSTA_REGISTRADA("Primeira resposta registrada", "Agente respondeu ao chamado pela primeira vez"),
    SLA_PRIMEIRA_RESPOSTA_CUMPRIDA("SLA de primeira resposta cumprido", "Primeira resposta dentro do prazo"),
    SLA_PRIMEIRA_RESPOSTA_VIOLADA("SLA de primeira resposta violado", "Primeira resposta fora do prazo"),
    SLA_RESOLUCAO_CUMPRIDA("SLA de resolução cumprido", "Chamado resolvido dentro do prazo"),
    SLA_RESOLUCAO_VIOLADA("SLA de resolução violado", "Chamado resolvido fora do prazo"),
    SLA_RECALCULADO("SLA recalculado", "Prazos de SLA recalculados devido a mudança de prioridade"),
    SLA_CANCELADO("SLA cancelado", "SLA encerrado sem resolução");

    private final String label;
    private final String description;

    TicketEventType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * Verifica se é um evento relacionado a comentário.
     */
    public boolean isCommentEvent() {
        return this == COMENTARIO_ADICIONADO ||
               this == COMENTARIO_EDITADO ||
               this == COMENTARIO_REMOVIDO;
    }

    /**
     * Verifica se é um evento de alteração de dados.
     */
    public boolean isChangeEvent() {
        return this == STATUS_ALTERADO ||
               this == PRIORIDADE_ALTERADA ||
               this == CAMPO_ALTERADO ||
               this == RESPONSAVEL_ATRIBUIDO ||
               this == RESPONSAVEL_ALTERADO ||
               this == RESPONSAVEL_REMOVIDO;
    }

    /**
     * Verifica se é um evento de SLA.
     */
    public boolean isSlaEvent() {
        return this == SLA_EM_RISCO || this == SLA_VENCIDO
                || this == SLA_PRIMEIRA_RESPOSTA_REGISTRADA
                || this == SLA_PRIMEIRA_RESPOSTA_CUMPRIDA
                || this == SLA_PRIMEIRA_RESPOSTA_VIOLADA
                || this == SLA_RESOLUCAO_CUMPRIDA
                || this == SLA_RESOLUCAO_VIOLADA
                || this == SLA_RECALCULADO
                || this == SLA_CANCELADO;
    }

    /**
     * Verifica se é um evento de ciclo de vida final.
     */
    public boolean isLifecycleEndEvent() {
        return this == CHAMADO_RESOLVIDO ||
               this == CHAMADO_FECHADO ||
               this == CHAMADO_CANCELADO;
    }
}
