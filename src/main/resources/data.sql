-- ---------------------------------------------------------------------------
-- Massa de dados do desafio. Recriada a cada start da aplicação.
-- Não altere os UUIDs nem os SKUs: a bateria de aceite depende deles.
-- ---------------------------------------------------------------------------

-- Catálogo -------------------------------------------------------------------
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-CABO-USB',  'Cabo USB-C 1m',                   1000, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-ADAPTADOR', 'Adaptador HDMI (descontinuado)',  3990, FALSE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-MOUSE',     'Mouse sem fio',                   4990, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-SUPORTE',   'Suporte para notebook',           8990, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-TECLADO',   'Teclado mecânico',               11990, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-WEBCAM',    'Webcam HD',                      15900, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-HEADSET',   'Headset com microfone',          25990, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-DOCK',      'Dock station USB-C',             45900, TRUE);
INSERT INTO product (sku, name, price_cents, active) VALUES ('SKU-MONITOR',   'Monitor 24 polegadas',           89900, TRUE);

-- Carteiras de demonstração (use estas para testar na mão) --------------------
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000001' AS UUID), 'Ana Souza',  50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000002' AS UUID), 'Bruno Lima',  3000);

-- Carteiras reservadas para a bateria de aceite (não use nos seus testes) -----
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000011' AS UUID), 'Aceite 01 - feliz',              50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000012' AS UUID), 'Aceite 01 - saldo baixo',         1000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000013' AS UUID), 'Aceite 01 - diversos',           50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000021' AS UUID), 'Aceite 02 - replay',             50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000022' AS UUID), 'Aceite 02 - conflito',           50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000023' AS UUID), 'Aceite 02 - chaves distintas',   50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000031' AS UUID), 'Aceite 03 - cancela',            50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000032' AS UUID), 'Aceite 03 - cancela 2x',         50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000033' AS UUID), 'Aceite 03 - janela expirada',    50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000034' AS UUID), 'Aceite 03 - janela válida',      50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000035' AS UUID), 'Aceite 03 - cancelado+expirado', 50000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000041' AS UUID), 'Aceite 04 - paginação',         100000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000042' AS UUID), 'Aceite 04 - filtro status',     100000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000043' AS UUID), 'Aceite 04 - ordenação',         100000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000051' AS UUID), 'Aceite 05 - concorrência',       10000);
INSERT INTO wallet (id, owner_name, balance_cents) VALUES (CAST('00000000-0000-0000-0000-000000000061' AS UUID), 'Aceite 06 - N+1',               500000);
