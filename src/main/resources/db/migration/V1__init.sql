-- Flyway versiona el esquema igual que Git versiona el código:
-- cada cambio es un archivo numerado, inmutable, que se aplica una sola vez.
-- El pipeline de CI/CD ejecuta estas migraciones automáticamente antes de desplegar.

CREATE TABLE cuentas (
    id              BIGSERIAL PRIMARY KEY,
    numero_cuenta   VARCHAR(20)     NOT NULL UNIQUE,
    titular         VARCHAR(150)    NOT NULL,
    saldo           NUMERIC(15, 2)  NOT NULL DEFAULT 0,
    moneda          VARCHAR(3)      NOT NULL DEFAULT 'PEN',
    creado_en       TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE transferencias (
    id                  BIGSERIAL PRIMARY KEY,
    clave_idempotencia  VARCHAR(100)    NOT NULL UNIQUE,
    cuenta_origen_id    BIGINT          NOT NULL REFERENCES cuentas(id),
    cuenta_destino_id   BIGINT          NOT NULL REFERENCES cuentas(id),
    monto               NUMERIC(15, 2)  NOT NULL,
    moneda              VARCHAR(3)      NOT NULL,
    estado              VARCHAR(20)     NOT NULL,
    motivo_rechazo      VARCHAR(200),
    creado_en           TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE TABLE movimientos (
    id                  BIGSERIAL PRIMARY KEY,
    cuenta_id           BIGINT          NOT NULL REFERENCES cuentas(id),
    transferencia_id    BIGINT          NOT NULL REFERENCES transferencias(id),
    tipo                VARCHAR(10)     NOT NULL, -- DEBITO | CREDITO
    monto               NUMERIC(15, 2)  NOT NULL,
    saldo_posterior     NUMERIC(15, 2)  NOT NULL,
    creado_en           TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimientos_cuenta ON movimientos(cuenta_id);
CREATE INDEX idx_transferencias_cuenta_origen ON transferencias(cuenta_origen_id);

-- Datos de prueba para el entorno local / demo
INSERT INTO cuentas (numero_cuenta, titular, saldo, moneda) VALUES
    ('001-0001', 'James Llalle',   1500.00, 'PEN'),
    ('001-0002', 'Ana Torres',      300.00, 'PEN'),
    ('001-0003', 'Luis Paredes',      0.00, 'PEN');
