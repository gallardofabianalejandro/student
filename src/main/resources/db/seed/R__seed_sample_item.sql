-- Datos de ejemplo SOLO para el perfil local (db/seed no se carga en otros perfiles).
-- Migracion repetible e idempotente: reaplicarla no duplica filas.
INSERT INTO sample_item (id, name, description, quantity, price, created_by, created_date)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'Teclado mecanico', 'Dato de ejemplo local', 10, 89.90, 'seed', now()),
    ('00000000-0000-0000-0000-000000000102', 'Mouse inalambrico', 'Dato de ejemplo local', 25, 24.50, 'seed', now()),
    ('00000000-0000-0000-0000-000000000103', 'Monitor 27 pulgadas', 'Dato de ejemplo local', 4, 289.00, 'seed', now())
ON CONFLICT (name) DO NOTHING;
