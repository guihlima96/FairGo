ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS versao_nome bigint NOT NULL DEFAULT 1;

CREATE OR REPLACE FUNCTION versionar_nome_usuario() RETURNS trigger LANGUAGE plpgsql AS '
BEGIN
    NEW.versao_nome := OLD.versao_nome + 1;
    RETURN NEW;
END';
DO 'BEGIN
    IF NOT EXISTS (SELECT FROM pg_trigger WHERE tgrelid=''usuarios''::regclass AND tgname=''mudanca_nome_usuario'') THEN
        CREATE TRIGGER mudanca_nome_usuario BEFORE UPDATE OF nome ON usuarios
        FOR EACH ROW WHEN (OLD.nome IS DISTINCT FROM NEW.nome) EXECUTE FUNCTION versionar_nome_usuario();
    END IF;
END';

CREATE TABLE IF NOT EXISTS conferencias_identidade (
    sequencia bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    motorista_id uuid NOT NULL REFERENCES usuarios(id),
    versao_nome bigint NOT NULL,
    nome_conferido text NOT NULL,
    revisao_cnh uuid NOT NULL,
    revisao_foto uuid NOT NULL,
    resultado text NOT NULL CHECK (resultado IN ('CONFERE','DIVERGENTE')),
    responsavel text NOT NULL,
    data timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX IF NOT EXISTS conferencias_identidade_titular ON conferencias_identidade(motorista_id, sequencia DESC);

CREATE OR REPLACE VIEW identidade_motorista_atual AS
SELECT u.id, u.versao_nome, c.id AS revisao_cnh, f.id AS revisao_foto,
       COALESCE(e.resultado='CONFERE' AND e.versao_nome=u.versao_nome AND
                e.revisao_cnh=c.id AND e.revisao_foto=f.id, false) AS verificada
FROM usuarios u
LEFT JOIN documentos_cadastro c ON c.motorista_id=u.id AND c.tipo='CNH'
LEFT JOIN documentos_cadastro f ON f.motorista_id=u.id AND f.tipo='FOTO_PERFIL'
LEFT JOIN LATERAL (
    SELECT resultado, versao_nome, revisao_cnh, revisao_foto FROM conferencias_identidade
    WHERE motorista_id=u.id ORDER BY sequencia DESC LIMIT 1
) e ON true
WHERE u.perfil='MOTORISTA';
