-- =========================================================
-- V75: Tornar admin.lead.mensagem opcional
--
-- Quando o admin cria um lead manualmente pelo painel, ele pode ainda
-- nao ter conversado com o cliente — a mensagem pode chegar vazia.
-- O endpoint publico /api/contato continua validando min=10 chars.
-- =========================================================

ALTER TABLE admin.lead ALTER COLUMN mensagem DROP NOT NULL;
