-- Dummy Data ?

-- Tipos de Grupos

-- Papéis
INSERT INTO papel (nome) VALUES ('ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO papel (nome) VALUES ('COORDENADOR') ON CONFLICT DO NOTHING; -- docente
INSERT INTO papel (nome) VALUES ('DIRETOR') ON CONFLICT DO NOTHING; -- discente
INSERT INTO papel (nome) VALUES ('VICE-DIRETOR') ON CONFLICT DO NOTHING; -- discente
INSERT INTO papel (nome) VALUES ('TESOUREIRO') ON CONFLICT DO NOTHING; -- discente
-- criar cargo p/ membro
