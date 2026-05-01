-- Módulo 4: Anexos de Chamados
-- Tabela de metadados de anexos com soft delete e auditoria completa
-- Nota: os novos valores do enum event_type foram adicionados na V5.1

-- Tabela de Anexos de Chamados
CREATE TABLE ticket_attachments (
    id                 UUID         PRIMARY KEY,
    ticket_id          UUID         NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,

    -- Metadados do arquivo
    file_name          VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type       VARCHAR(127) NOT NULL,
    file_size          BIGINT       NOT NULL,
    storage_path       VARCHAR(512) NOT NULL,

    -- Auditoria de criação
    uploaded_by        UUID         NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP    NOT NULL,

    -- Soft delete
    removed            BOOLEAN      NOT NULL DEFAULT FALSE,
    removed_at         TIMESTAMP,
    removed_by         UUID         REFERENCES users(id),

    -- Constraints
    CONSTRAINT chk_file_size_positive        CHECK (file_size > 0),
    CONSTRAINT chk_file_name_not_empty       CHECK (LENGTH(TRIM(file_name)) > 0),
    CONSTRAINT chk_original_name_not_empty   CHECK (LENGTH(TRIM(original_file_name)) > 0),
    CONSTRAINT chk_storage_path_not_empty    CHECK (LENGTH(TRIM(storage_path)) > 0),
    CONSTRAINT chk_removed_consistency       CHECK (
        (removed = FALSE AND removed_at IS NULL AND removed_by IS NULL)
        OR
        (removed = TRUE AND removed_at IS NOT NULL)
    )
);

-- Índices principais
CREATE INDEX idx_ticket_attachments_ticket     ON ticket_attachments(ticket_id);
CREATE INDEX idx_ticket_attachments_uploaded   ON ticket_attachments(uploaded_by);
CREATE INDEX idx_ticket_attachments_removed    ON ticket_attachments(ticket_id, removed)
    WHERE removed = FALSE;
CREATE INDEX idx_ticket_attachments_created    ON ticket_attachments(ticket_id, created_at);

-- Comentários
COMMENT ON TABLE ticket_attachments                           IS 'Metadados de anexos dos chamados (conteúdo binário fica no storage)';
COMMENT ON COLUMN ticket_attachments.id                      IS 'Identificador único do anexo (UUID)';
COMMENT ON COLUMN ticket_attachments.ticket_id               IS 'Referência ao chamado';
COMMENT ON COLUMN ticket_attachments.file_name               IS 'Nome gerado para armazenamento seguro';
COMMENT ON COLUMN ticket_attachments.original_file_name      IS 'Nome original enviado pelo usuário';
COMMENT ON COLUMN ticket_attachments.content_type            IS 'MIME type do arquivo';
COMMENT ON COLUMN ticket_attachments.file_size               IS 'Tamanho em bytes';
COMMENT ON COLUMN ticket_attachments.storage_path            IS 'Caminho relativo no storage (não expor na API pública)';
COMMENT ON COLUMN ticket_attachments.uploaded_by             IS 'Usuário que fez o upload';
COMMENT ON COLUMN ticket_attachments.created_at              IS 'Data/hora do upload';
COMMENT ON COLUMN ticket_attachments.removed                 IS 'Soft delete: TRUE indica que o anexo foi removido logicamente';
COMMENT ON COLUMN ticket_attachments.removed_at              IS 'Data/hora da remoção lógica';
COMMENT ON COLUMN ticket_attachments.removed_by              IS 'Usuário que removeu o anexo';
