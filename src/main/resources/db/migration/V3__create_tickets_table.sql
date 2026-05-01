-- Tabela de Chamados (Tickets)
-- Modelagem completa com relacionamentos, índices e constraints

CREATE TYPE ticket_status AS ENUM (
    'ABERTO',
    'EM_TRIAGEM',
    'EM_ATENDIMENTO',
    'AGUARDANDO_SOLICITANTE',
    'RESOLVIDO',
    'FECHADO',
    'CANCELADO'
);

CREATE TYPE ticket_priority AS ENUM (
    'BAIXA',
    'MEDIA',
    'ALTA',
    'CRITICA'
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status ticket_status NOT NULL DEFAULT 'ABERTO',
    priority ticket_priority NOT NULL DEFAULT 'MEDIA',
    category VARCHAR(100),
    
    -- Relacionamentos com usuários
    requester_id UUID NOT NULL REFERENCES users(id),
    assignee_id UUID REFERENCES users(id),
    
    -- Datas de controle
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    sla_due_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    
    -- Constraints adicionais
    CONSTRAINT chk_title_not_empty CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_description_not_empty CHECK (LENGTH(TRIM(description)) > 0)
);

-- Índices para consultas frequentes
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_requester ON tickets(requester_id);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);
CREATE INDEX idx_tickets_sla_due ON tickets(sla_due_at);

-- Índice composto para consultas comuns (status + prioridade)
CREATE INDEX idx_tickets_status_priority ON tickets(status, priority);

-- Índice para busca de chamados abertos por solicitante
CREATE INDEX idx_tickets_requester_status ON tickets(requester_id, status);

-- Índice para busca de chamados atribuídos a um responsável
CREATE INDEX idx_tickets_assignee_status ON tickets(assignee_id, status);

-- Comentários na tabela (documentação)
COMMENT ON TABLE tickets IS 'Tabela principal de chamados/tickets do sistema de ITSM';
COMMENT ON COLUMN tickets.id IS 'Identificador único do chamado (UUID)';
COMMENT ON COLUMN tickets.title IS 'Título do chamado (máx 200 caracteres)';
COMMENT ON COLUMN tickets.description IS 'Descrição detalhada do chamado';
COMMENT ON COLUMN tickets.status IS 'Status atual do chamado (enum)';
COMMENT ON COLUMN tickets.priority IS 'Prioridade do chamado (enum)';
COMMENT ON COLUMN tickets.category IS 'Categoria/tipo do chamado';
COMMENT ON COLUMN tickets.requester_id IS 'Usuário que criou o chamado (solicitante)';
COMMENT ON COLUMN tickets.assignee_id IS 'Usuário responsável pelo atendimento (pode ser nulo)';
COMMENT ON COLUMN tickets.created_at IS 'Data/hora de criação do chamado';
COMMENT ON COLUMN tickets.updated_at IS 'Data/hora da última atualização';
COMMENT ON COLUMN tickets.sla_due_at IS 'Data/hora limite do SLA baseado na prioridade';
COMMENT ON COLUMN tickets.resolved_at IS 'Data/hora de resolução (preenchido ao resolver)';
