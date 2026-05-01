-- Módulo 3: Comentários e Histórico de Chamados
-- Criação de tabelas para timeline, comentários e eventos de auditoria

-- Enum para visibilidade (reutilizado em comentários e eventos)
CREATE TYPE visibility_type AS ENUM ('PUBLICO', 'INTERNO');

-- Enum para tipos de evento de histórico
CREATE TYPE event_type AS ENUM (
    'CHAMADO_CRIADO',
    'COMENTARIO_ADICIONADO',
    'COMENTARIO_EDITADO',
    'COMENTARIO_REMOVIDO',
    'STATUS_ALTERADO',
    'PRIORIDADE_ALTERADA',
    'RESPONSAVEL_ATRIBUIDO',
    'RESPONSAVEL_ALTERADO',
    'RESPONSAVEL_REMOVIDO',
    'SLA_EM_RISCO',
    'SLA_VENCIDO',
    'CHAMADO_RESOLVIDO',
    'CHAMADO_FECHADO',
    'CHAMADO_CANCELADO',
    'CAMPO_ALTERADO'
);

-- Enum para origem do evento
CREATE TYPE event_origin AS ENUM ('USUARIO', 'SISTEMA');

-- Tabela de Comentários de Chamados
CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    visibility visibility_type NOT NULL DEFAULT 'PUBLICO',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    removed BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_content_not_empty CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT chk_content_max_length CHECK (LENGTH(content) <= 5000)
);

-- Tabela de Eventos de Histórico de Chamados
CREATE TABLE ticket_events (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    event_type event_type NOT NULL,
    actor_id UUID REFERENCES users(id), -- pode ser nulo para eventos do sistema
    origin event_origin NOT NULL DEFAULT 'USUARIO',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    metadata JSONB, -- dados flexíveis para extensibilidade
    visibility visibility_type NOT NULL DEFAULT 'PUBLICO',
    created_at TIMESTAMP NOT NULL
);

-- Índices para Comentários
CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id);
CREATE INDEX idx_ticket_comments_author ON ticket_comments(author_id);
CREATE INDEX idx_ticket_comments_created_at ON ticket_comments(created_at);
CREATE INDEX idx_ticket_comments_visibility ON ticket_comments(visibility);
CREATE INDEX idx_ticket_comments_ticket_visibility ON ticket_comments(ticket_id, visibility);
CREATE INDEX idx_ticket_comments_removed ON ticket_comments(removed) WHERE removed = FALSE;

-- Índices para Eventos
CREATE INDEX idx_ticket_events_ticket ON ticket_events(ticket_id);
CREATE INDEX idx_ticket_events_actor ON ticket_events(actor_id);
CREATE INDEX idx_ticket_events_created_at ON ticket_events(created_at);
CREATE INDEX idx_ticket_events_event_type ON ticket_events(event_type);
CREATE INDEX idx_ticket_events_visibility ON ticket_events(visibility);
CREATE INDEX idx_ticket_events_ticket_created ON ticket_events(ticket_id, created_at);
CREATE INDEX idx_ticket_events_ticket_visibility ON ticket_events(ticket_id, visibility, created_at);

-- Índice GIN para metadata JSONB (busca eficiente em JSON)
CREATE INDEX idx_ticket_events_metadata ON ticket_events USING GIN (metadata jsonb_path_ops);

-- Comentários nas tabelas e colunas (documentação)
COMMENT ON TABLE ticket_comments IS 'Tabela de comentários em chamados com soft delete e controle de visibilidade';
COMMENT ON COLUMN ticket_comments.id IS 'Identificador único do comentário (UUID)';
COMMENT ON COLUMN ticket_comments.ticket_id IS 'Referência ao chamado (chave estrangeira)';
COMMENT ON COLUMN ticket_comments.author_id IS 'Autor do comentário (chave estrangeira)';
COMMENT ON COLUMN ticket_comments.content IS 'Conteúdo textual do comentário (máx 5000 caracteres)';
COMMENT ON COLUMN ticket_comments.visibility IS 'Visibilidade: PUBLICO (todos) ou INTERNO (equipe)';
COMMENT ON COLUMN ticket_comments.edited IS 'Indica se o comentário foi editado';
COMMENT ON COLUMN ticket_comments.removed IS 'Soft delete - indica se o comentário foi removido logicamente';

COMMENT ON TABLE ticket_events IS 'Tabela de eventos de histórico para auditoria e timeline';
COMMENT ON COLUMN ticket_events.id IS 'Identificador único do evento (UUID)';
COMMENT ON COLUMN ticket_events.ticket_id IS 'Referência ao chamado (chave estrangeira)';
COMMENT ON COLUMN ticket_events.event_type IS 'Tipo de evento (enum)';
COMMENT ON COLUMN ticket_events.actor_id IS 'Usuário que causou o evento (nulo para sistema)';
COMMENT ON COLUMN ticket_events.origin IS 'Origem: USUARIO ou SISTEMA';
COMMENT ON COLUMN ticket_events.title IS 'Título legível do evento para timeline';
COMMENT ON COLUMN ticket_events.description IS 'Descrição detalhada do evento';
COMMENT ON COLUMN ticket_events.old_value IS 'Valor anterior (para eventos de alteração)';
COMMENT ON COLUMN ticket_events.new_value IS 'Novo valor (para eventos de alteração)';
COMMENT ON COLUMN ticket_events.metadata IS 'Dados adicionais em formato JSONB';
COMMENT ON COLUMN ticket_events.visibility IS 'Visibilidade: PUBLICO (todos) ou INTERNO (equipe)';
