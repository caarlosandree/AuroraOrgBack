-- Módulo 6: SLA Simples
-- Adiciona campos de SLA avançado à tabela de chamados e cria tabela de políticas configuráveis

-- Tipos ENUM para SLA
CREATE TYPE sla_status AS ENUM (
    'DENTRO_DO_PRAZO',
    'EM_RISCO',
    'VENCIDO',
    'CUMPRIDO',
    'VIOLADO',
    'CANCELADO',
    'PAUSADO'
);

-- Adiciona campos de SLA na tabela tickets
ALTER TABLE tickets
    ADD COLUMN prazo_primeira_resposta TIMESTAMP,
    ADD COLUMN prazo_resolucao TIMESTAMP,
    ADD COLUMN data_primeira_resposta TIMESTAMP,
    ADD COLUMN data_resolucao TIMESTAMP,
    ADD COLUMN sla_primeira_resposta_status sla_status DEFAULT 'DENTRO_DO_PRAZO',
    ADD COLUMN sla_resolucao_status sla_status DEFAULT 'DENTRO_DO_PRAZO',
    ADD COLUMN primeira_resposta_por UUID REFERENCES users(id),
    ADD COLUMN tempo_primeira_resposta_segundos BIGINT,
    ADD COLUMN tempo_resolucao_segundos BIGINT;

-- Índices para consultas de monitoramento SLA
CREATE INDEX idx_tickets_sla_primeira_resposta_status ON tickets(sla_primeira_resposta_status);
CREATE INDEX idx_tickets_sla_resolucao_status ON tickets(sla_resolucao_status);
CREATE INDEX idx_tickets_prazo_resolucao ON tickets(prazo_resolucao);
CREATE INDEX idx_tickets_prazo_primeira_resposta ON tickets(prazo_primeira_resposta);

-- Índice composto para busca eficiente de chamados em risco/vencidos abertos
CREATE INDEX idx_tickets_status_sla_resolucao ON tickets(status, sla_resolucao_status);
CREATE INDEX idx_tickets_status_sla_primeira_resposta ON tickets(status, sla_primeira_resposta_status);

-- Tabela de políticas de SLA (preparada para customização futura)
CREATE TABLE sla_politicas (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    prioridade ticket_priority NOT NULL,
    prazo_primeira_resposta_minutos INTEGER NOT NULL,
    prazo_resolucao_minutos INTEGER NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT true,
    fila_id UUID REFERENCES filas(id),
    categoria_id UUID REFERENCES categorias(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_sla_politica_nome_not_empty CHECK (LENGTH(TRIM(nome)) > 0),
    CONSTRAINT chk_sla_politica_pr_resposta_positivo CHECK (prazo_primeira_resposta_minutos > 0),
    CONSTRAINT chk_sla_politica_pr_resolucao_positivo CHECK (prazo_resolucao_minutos > 0)
);

-- Índices para políticas de SLA
CREATE INDEX idx_sla_politicas_ativa ON sla_politicas(ativa);
CREATE INDEX idx_sla_politicas_prioridade ON sla_politicas(prioridade);
CREATE INDEX idx_sla_politicas_fila ON sla_politicas(fila_id);
CREATE INDEX idx_sla_politicas_categoria ON sla_politicas(categoria_id);

-- Constraint única para evitar políticas duplicadas ativas para mesma prioridade/fila/categoria
CREATE UNIQUE INDEX idx_sla_politicas_unica_ativa
    ON sla_politicas (prioridade, COALESCE(fila_id, '00000000-0000-0000-0000-000000000000'), COALESCE(categoria_id, '00000000-0000-0000-0000-000000000000'))
    WHERE ativa = true;

-- Atualiza dados existentes: preenche prazo_resolucao com sla_due_at existente
UPDATE tickets
SET prazo_resolucao = sla_due_at,
    sla_resolucao_status = CASE
        WHEN status IN ('RESOLVIDO', 'FECHADO') AND resolved_at IS NOT NULL AND resolved_at <= sla_due_at THEN 'CUMPRIDO'
        WHEN status IN ('RESOLVIDO', 'FECHADO') AND resolved_at IS NOT NULL AND resolved_at > sla_due_at THEN 'VIOLADO'
        WHEN status = 'CANCELADO' THEN 'CANCELADO'
        WHEN sla_due_at < NOW() THEN 'VENCIDO'
        ELSE 'DENTRO_DO_PRAZO'
    END,
    sla_primeira_resposta_status = CASE
        WHEN status = 'CANCELADO' THEN 'CANCELADO'
        ELSE 'DENTRO_DO_PRAZO'
    END;

-- Comentários
COMMENT ON TYPE sla_status IS 'Status possíveis do SLA de um chamado';

COMMENT ON COLUMN tickets.prazo_primeira_resposta IS 'Data/hora limite para primeira resposta do agente';
COMMENT ON COLUMN tickets.prazo_resolucao IS 'Data/hora limite para resolução do chamado';
COMMENT ON COLUMN tickets.data_primeira_resposta IS 'Data/hora em que o agente respondeu pela primeira vez';
COMMENT ON COLUMN tickets.data_resolucao IS 'Data/hora em que o chamado foi resolvido (pode diferir de resolved_at)';
COMMENT ON COLUMN tickets.sla_primeira_resposta_status IS 'Status do SLA de primeira resposta';
COMMENT ON COLUMN tickets.sla_resolucao_status IS 'Status do SLA de resolução';
COMMENT ON COLUMN tickets.primeira_resposta_por IS 'ID do usuário que deu a primeira resposta';
COMMENT ON COLUMN tickets.tempo_primeira_resposta_segundos IS 'Tempo decorrido em segundos até a primeira resposta';
COMMENT ON COLUMN tickets.tempo_resolucao_segundos IS 'Tempo decorrido em segundos até a resolução';

COMMENT ON TABLE sla_politicas IS 'Políticas configuráveis de SLA por prioridade, fila e categoria';
COMMENT ON COLUMN sla_politicas.prazo_primeira_resposta_minutos IS 'Prazo máximo em minutos para primeira resposta';
COMMENT ON COLUMN sla_politicas.prazo_resolucao_minutos IS 'Prazo máximo em minutos para resolução';
