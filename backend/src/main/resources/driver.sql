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
