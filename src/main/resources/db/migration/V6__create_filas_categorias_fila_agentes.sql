-- Módulo 5: Filas, Categorias e Atribuição
-- Cria tabelas para gerenciamento de filas de atendimento e categorias de chamados

-- Tabela de Filas de Atendimento
CREATE TABLE filas (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_fila_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- Índices para filas
CREATE INDEX idx_filas_active ON filas(active);
CREATE INDEX idx_filas_name ON filas(name);

-- Tabela de Categorias de Chamados
CREATE TABLE categorias (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    fila_padrao_id UUID REFERENCES filas(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT chk_categoria_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

-- Índices para categorias
CREATE INDEX idx_categorias_active ON categorias(active);
CREATE INDEX idx_categorias_name ON categorias(name);
CREATE INDEX idx_categorias_fila_padrao ON categorias(fila_padrao_id);

-- Tabela de relacionamento Filas <-> Agentes (N:N)
CREATE TABLE fila_agentes (
    fila_id UUID NOT NULL REFERENCES filas(id) ON DELETE CASCADE,
    agente_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,

    PRIMARY KEY (fila_id, agente_id)
);

-- Índices para fila_agentes
CREATE INDEX idx_fila_agentes_fila ON fila_agentes(fila_id);
CREATE INDEX idx_fila_agentes_agente ON fila_agentes(agente_id);

-- Ajustes na tabela de chamados para suportar fila e categoria
ALTER TABLE tickets
    ADD COLUMN fila_id UUID REFERENCES filas(id),
    ADD COLUMN categoria_id UUID REFERENCES categorias(id);

-- Índices para as novas colunas de tickets
CREATE INDEX idx_tickets_fila ON tickets(fila_id);
CREATE INDEX idx_tickets_categoria ON tickets(categoria_id);

-- Comentários
COMMENT ON TABLE filas IS 'Filas de atendimento para distribuição de chamados';
COMMENT ON COLUMN filas.id IS 'Identificador único da fila';
COMMENT ON COLUMN filas.name IS 'Nome da fila (ex: Suporte Técnico, Financeiro)';
COMMENT ON COLUMN filas.description IS 'Descrição da fila';
COMMENT ON COLUMN filas.active IS 'Indica se a fila está ativa';

COMMENT ON TABLE categorias IS 'Categorias de classificação de chamados';
COMMENT ON COLUMN categorias.id IS 'Identificador único da categoria';
COMMENT ON COLUMN categorias.name IS 'Nome da categoria (ex: Acesso, Erro no sistema)';
COMMENT ON COLUMN categorias.description IS 'Descrição da categoria';
COMMENT ON COLUMN categorias.active IS 'Indica se a categoria está ativa';
COMMENT ON COLUMN categorias.fila_padrao_id IS 'Fila padrão para chamados desta categoria';

COMMENT ON TABLE fila_agentes IS 'Relacionamento N:N entre filas e agentes';
COMMENT ON COLUMN fila_agentes.fila_id IS 'Referência à fila';
COMMENT ON COLUMN fila_agentes.agente_id IS 'Referência ao usuário agente';

COMMENT ON COLUMN tickets.fila_id IS 'Fila atual do chamado';
COMMENT ON COLUMN tickets.categoria_id IS 'Categoria do chamado';
