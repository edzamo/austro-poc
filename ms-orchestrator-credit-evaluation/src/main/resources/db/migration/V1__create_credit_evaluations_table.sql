-- Migración V1: Creación de la tabla principal de evaluaciones de crédito
-- Usa tipos explícitos y constraints para garantizar integridad de datos.

CREATE TABLE IF NOT EXISTS credit_evaluations (
    id               BIGSERIAL       PRIMARY KEY,
    cedula           VARCHAR(10)     NOT NULL,
    requested_amount NUMERIC(15, 2)  NOT NULL,
    years            INTEGER         NOT NULL,
    salary           NUMERIC(15, 2)  NOT NULL,
    final_status     VARCHAR(10)     NOT NULL CHECK (final_status IN ('APROBADO', 'RECHAZADO')),
    evaluation_date  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_credit_evaluations_cedula
    ON credit_evaluations (cedula);

CREATE INDEX IF NOT EXISTS idx_credit_evaluations_date
    ON credit_evaluations (evaluation_date DESC);

COMMENT ON TABLE credit_evaluations IS 'Registro de todas las evaluaciones de crédito procesadas por el sistema';
COMMENT ON COLUMN credit_evaluations.cedula IS 'Cédula de identidad del solicitante (validada con Módulo 10)';
COMMENT ON COLUMN credit_evaluations.final_status IS 'Resultado de la evaluación: APROBADO o RECHAZADO';
