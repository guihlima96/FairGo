CREATE TABLE IF NOT EXISTS usuarios (
    id uuid PRIMARY KEY,
    nome text NOT NULL,
    email text NOT NULL UNIQUE,
    perfil text NOT NULL CHECK (perfil IN ('PASSAGEIRO', 'MOTORISTA')),
    situacao text NOT NULL DEFAULT 'PENDENTE'
);
CREATE TABLE IF NOT EXISTS veiculos (
    id uuid PRIMARY KEY,
    motorista_id uuid NOT NULL REFERENCES usuarios(id),
    placa text NOT NULL UNIQUE,
    situacao text NOT NULL DEFAULT 'PENDENTE'
);
