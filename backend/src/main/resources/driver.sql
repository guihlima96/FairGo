CREATE TABLE IF NOT EXISTS contas_driver (
    usuario_id uuid PRIMARY KEY REFERENCES usuarios(id),
    cpf char(11) NOT NULL UNIQUE,
    celular text NOT NULL,
    senha_hash text NOT NULL,
    nome_verificado boolean NOT NULL DEFAULT false,
    email_verificado boolean NOT NULL DEFAULT false,
    celular_verificado boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS sessoes_driver (
    digest char(64) PRIMARY KEY,
    usuario_id uuid NOT NULL REFERENCES contas_driver(usuario_id),
    expira_em timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS sessoes_driver_usuario ON sessoes_driver(usuario_id);
CREATE TABLE IF NOT EXISTS recuperacoes_driver (
    digest char(64) PRIMARY KEY,
    usuario_id uuid NOT NULL UNIQUE REFERENCES contas_driver(usuario_id),
    expira_em timestamptz NOT NULL
);
CREATE TABLE IF NOT EXISTS limites_driver (
    chave char(64) PRIMARY KEY,
    contador integer NOT NULL,
    ate timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS limites_driver_ate ON limites_driver(ate);

CREATE TABLE IF NOT EXISTS desafios_contato_driver (
    usuario_id uuid NOT NULL REFERENCES contas_driver(usuario_id),
    canal text NOT NULL CHECK (canal IN ('EMAIL','CELULAR')),
    desafio uuid NOT NULL,
    prova bytea NOT NULL CHECK (octet_length(prova)=32),
    expira_em timestamptz NOT NULL,
    reenviar_em timestamptz NOT NULL,
    fim_janela timestamptz NOT NULL,
    emissoes integer NOT NULL CHECK (emissoes BETWEEN 1 AND 5),
    restantes integer NOT NULL CHECK (restantes BETWEEN 0 AND 5),
    PRIMARY KEY (usuario_id, canal)
);

ALTER TABLE recuperacoes_driver ADD COLUMN IF NOT EXISTS vinculo char(64);

CREATE OR REPLACE FUNCTION invalidar_recuperacao_email() RETURNS trigger LANGUAGE plpgsql AS '
BEGIN
    DELETE FROM recuperacoes_driver WHERE usuario_id=NEW.usuario_id;
    RETURN NEW;
END';
CREATE OR REPLACE FUNCTION desverificar_email_alterado() RETURNS trigger LANGUAGE plpgsql AS '
BEGIN
    UPDATE contas_driver SET email_verificado=false WHERE usuario_id=NEW.id;
    RETURN NEW;
END';
DO 'BEGIN
    IF NOT EXISTS (SELECT FROM pg_trigger WHERE tgrelid=''contas_driver''::regclass AND tgname=''recuperacao_email_revogado'') THEN
        CREATE TRIGGER recuperacao_email_revogado AFTER UPDATE OF email_verificado ON contas_driver
        FOR EACH ROW WHEN (NOT NEW.email_verificado) EXECUTE FUNCTION invalidar_recuperacao_email();
    END IF;
    IF NOT EXISTS (SELECT FROM pg_trigger WHERE tgrelid=''usuarios''::regclass AND tgname=''recuperacao_email_alterado'') THEN
        CREATE TRIGGER recuperacao_email_alterado AFTER UPDATE OF email ON usuarios
        FOR EACH ROW WHEN (OLD.email IS DISTINCT FROM NEW.email) EXECUTE FUNCTION desverificar_email_alterado();
    END IF;
END';
