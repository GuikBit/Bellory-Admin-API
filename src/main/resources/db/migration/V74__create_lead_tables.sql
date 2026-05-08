-- =========================================================
-- V74: Modulo de Leads (formulario de contato publico + kanban admin)
-- Schema: admin
--
-- Tabelas:
--   admin.lead_status     - colunas configuraveis do kanban
--   admin.lead            - lead capturado do site ou criado manualmente
--   admin.lead_atividade  - historico unificado (auto + manual)
-- =========================================================

-- =====================
-- LEAD STATUS
-- =====================
CREATE TABLE admin.lead_status (
    id                  BIGSERIAL    PRIMARY KEY,
    codigo              VARCHAR(40)  NOT NULL UNIQUE,
    nome                VARCHAR(80)  NOT NULL,
    cor                 VARCHAR(7)   NOT NULL DEFAULT '#6B7280',
    ordem               INTEGER      NOT NULL DEFAULT 0,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    eh_status_inicial   BOOLEAN      NOT NULL DEFAULT FALSE,
    eh_status_final     BOOLEAN      NOT NULL DEFAULT FALSE,
    dt_criacao          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    dt_atualizacao      TIMESTAMPTZ
);

CREATE INDEX idx_lead_status_ordem ON admin.lead_status(ordem) WHERE ativo = TRUE;

-- Garantir no maximo 1 status inicial
CREATE UNIQUE INDEX uniq_lead_status_inicial
    ON admin.lead_status(eh_status_inicial)
    WHERE eh_status_inicial = TRUE;


-- =====================
-- LEAD
-- =====================
CREATE TABLE admin.lead (
    id                          UUID         PRIMARY KEY,
    status_id                   BIGINT       NOT NULL REFERENCES admin.lead_status(id),
    nome                        VARCHAR(100) NOT NULL,
    email                       VARCHAR(160) NOT NULL,
    telefone                    VARCHAR(20)  NOT NULL,
    tipo_negocio                VARCHAR(20)  NOT NULL,
    mensagem                    TEXT         NOT NULL,
    origem                      VARCHAR(120),
    prioridade                  VARCHAR(10)  NOT NULL DEFAULT 'MEDIA',
    tags                        TEXT[]       NOT NULL DEFAULT ARRAY[]::text[],
    valor_estimado              NUMERIC(12,2),
    data_prevista_fechamento    DATE,
    responsavel_id              BIGINT       REFERENCES admin.usuario_admin(id),
    ip_hash                     VARCHAR(64),
    user_agent                  TEXT,
    fill_time_ms                INTEGER,
    turnstile_ok                BOOLEAN      NOT NULL DEFAULT FALSE,
    policy_version              VARCHAR(20),
    dt_criacao                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    dt_atualizacao              TIMESTAMPTZ,
    deleted_at                  TIMESTAMPTZ
);

CREATE INDEX idx_lead_status_deleted    ON admin.lead(status_id, deleted_at);
CREATE INDEX idx_lead_responsavel       ON admin.lead(responsavel_id) WHERE responsavel_id IS NOT NULL;
CREATE INDEX idx_lead_email_lower       ON admin.lead(LOWER(email));
CREATE INDEX idx_lead_dt_criacao        ON admin.lead(dt_criacao DESC);
CREATE INDEX idx_lead_deleted_at        ON admin.lead(deleted_at) WHERE deleted_at IS NULL;


-- =====================
-- LEAD ATIVIDADE (historico)
-- =====================
CREATE TABLE admin.lead_atividade (
    id          BIGSERIAL    PRIMARY KEY,
    lead_id     UUID         NOT NULL REFERENCES admin.lead(id) ON DELETE CASCADE,
    tipo        VARCHAR(30)  NOT NULL,
    descricao   TEXT,
    dados       JSONB,
    autor_id    BIGINT       REFERENCES admin.usuario_admin(id),
    dt_criacao  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lead_atividade_lead ON admin.lead_atividade(lead_id, dt_criacao DESC);
CREATE INDEX idx_lead_atividade_tipo ON admin.lead_atividade(tipo);


-- =====================
-- SEED dos status iniciais do kanban
-- =====================
INSERT INTO admin.lead_status (codigo, nome, cor, ordem, eh_status_inicial, eh_status_final) VALUES
    ('NOVO',              'Novo',              '#3B82F6', 10, TRUE,  FALSE),
    ('EM_CONTATO',        'Em contato',        '#F59E0B', 20, FALSE, FALSE),
    ('QUALIFICADO',       'Qualificado',       '#8B5CF6', 30, FALSE, FALSE),
    ('PROPOSTA_ENVIADA',  'Proposta enviada',  '#06B6D4', 40, FALSE, FALSE),
    ('GANHO',             'Ganho',             '#10B981', 50, FALSE, TRUE),
    ('PERDIDO',           'Perdido',           '#EF4444', 60, FALSE, TRUE);
