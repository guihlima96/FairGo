CREATE TABLE IF NOT EXISTS credenciais_admin (
    digest char(64) PRIMARY KEY,
    admin_id text NOT NULL,
    expira_em timestamptz NOT NULL,
    revogada boolean NOT NULL DEFAULT false
);
CREATE TABLE IF NOT EXISTS limites_municipais (
    ibge char(7) PRIMARY KEY,
    idade_maxima integer NOT NULL CHECK (idade_maxima BETWEEN 0 AND 11)
);
CREATE TABLE IF NOT EXISTS dados_veiculo (
    veiculo_id uuid PRIMARY KEY REFERENCES veiculos,
    portas integer NOT NULL,
    ar_condicionado boolean NOT NULL,
    ano_fabricacao integer NOT NULL,
    municipio char(7) NOT NULL
);
CREATE TABLE IF NOT EXISTS documentos_cadastro (
    id uuid PRIMARY KEY,
    motorista_id uuid REFERENCES usuarios,
    veiculo_id uuid REFERENCES veiculos,
    tipo text NOT NULL,
    validade date,
    ear boolean,
    imagem_cifrada bytea NOT NULL,
    CHECK ((tipo = 'CRLV' AND veiculo_id IS NOT NULL AND motorista_id IS NULL)
        OR (tipo IN ('CNH','ANTECEDENTES','TOXICOLOGICO') AND motorista_id IS NOT NULL AND veiculo_id IS NULL)),
    UNIQUE (motorista_id, tipo),
    UNIQUE (veiculo_id, tipo)
);
CREATE TABLE IF NOT EXISTS decisoes_cadastro (
    sequencia bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    motorista_id uuid REFERENCES usuarios,
    veiculo_id uuid REFERENCES veiculos,
    CHECK (num_nonnulls(motorista_id, veiculo_id) = 1),
    situacao text NOT NULL CHECK (situacao IN ('APROVADO','REJEITADO','PENDENTE')),
    motivo text NOT NULL,
    responsavel text NOT NULL,
    data timestamptz NOT NULL DEFAULT clock_timestamp()
);
