-- Papéis
INSERT INTO papel (id_papel, nome) VALUES
                                       (1, 'ADMIN'),
                                       (2, 'COORDENADOR'),
                                       (3, 'DIRETOR'),
                                       (4, 'VICE-DIRETOR'),
                                       (5, 'TESOUREIRO'),
                                       (6, 'MEMBRO');

-- Tipos de Oportunidade
INSERT INTO tipo (id_tipo, nome) VALUES
                                     (1, 'PROJETO'),
                                     (2, 'CURSO'),
                                     (3, 'EVENTO'),
                                     (4, 'PROGRAMA'),
                                     (5, 'PRESTACAO_SERVICO');

-- Curso
INSERT INTO curso (id_curso, nome, codigo, curriculo,
                   carga_horaria, data_inicio, data_fim) VALUES
                                                             (1, 'Ciência da Computação', 'CC001',
                                                              '2025.1', 3200, '2025-01-01',
                                                              NULL),
                                                             (2, 'Ciência da Computaçãoo', 'CC002',
                                                              '2018.2', 3000,
                                                              '2018-08-01', '2024-12-31');


-- Usuário
INSERT INTO usuario (id_usuario, nome, email, senha, ativo) VALUES
-- docentes + admin
(1, 'Maria Souza Lima', 'maria.lima@ufma.br', '$2a$10$senhaCriptografada1', true),
(2, 'João Pereira Alves', 'joao.alves@ufma.br', '$2a$10$senhaCriptografada2', true),
(3, 'Ana Carolina Ribeiro', 'ana.ribeiro@ufma.br', '$2a$10$senhaCriptografada3', true),
-- discentes
(4, 'Pedro Henrique Costa', 'pedro.costa@discente.ufma.br', '$2a$10$senhaCriptografada4', true),
(5, 'Larissa Fernandes Dias', 'larissa.dias@discente.ufma.br', '$2a$10$senhaCriptografada5', true),
(6, 'Gabriel Nunes Santos', 'gabriel.santos@discente.ufma.br', '$2a$10$senhaCriptografada6', true),
(7, 'Beatriz Almeida Rocha', 'beatriz.rocha@discente.ufma.br', '$2a$10$senhaCriptografada7', true),
(8, 'Lucas Martins Oliveira', 'lucas.oliveira@discente.ufma.br', '$2a$10$senhaCriptografada8', false);

-- Inserido papel em usuário
INSERT INTO usuario_papel (id_usuario, id_papel) VALUES
(1, 2),          -- Maria: coordenadora
(2, 2),          -- João: coordenador
(3, 1),          -- Ana: admin
(5, 3);          -- Larissa: discente diretor


-- Docente
INSERT INTO docente (id_usuario, siape, departamento) VALUES
(1, '1234567', 'Departamento de Informática'),
(2, '2345678', 'Departamento de Informática');

-- Discente
INSERT INTO discente (id_usuario, matricula, semestre_atual, carga_horaria, id_curso) VALUES
(4, '2025001234', 6, 120, 1),
(5, '2020004567', 8, 180, 2),
(6, '2025007890', 4, 60,  1),
(7, '2021003456', 5, 90,  2),
(8, '2019009876', 10, 220, 2);

-- Oportunidade
INSERT INTO oportunidade (id_oportunidade, titulo, descricao, carga_horaria, vagas, vagas_livres,
                          data_inicio, data_fim, status, id_curso, id_tipo, id_usuario) VALUES
(1, 'Monitoria de Estrutura de Dados', 'Monitoria da disciplina de Estrutura de Dados', 60, 2, 1,
 '2026-02-01', '2026-06-30', 'EM_EXECUCAO', 1, 2, 1),

(2, 'Projeto de Extensão - Robótica nas Escolas', 'Oficinas de robótica em escolas públicas', 120, 10, 6,
 '2026-03-01', '2026-11-30', 'ABERTA', 2, 1, 2),

(3, 'Curso de Introdução à IA', 'Curso de extensão sobre fundamentos de inteligência artificial', 40, 30, 30,
 '2026-08-01', '2026-08-30', 'ABERTA', 1, 2, 2),

(4, 'Estágio em Desenvolvimento Web', 'Vaga de estágio para desenvolvimento de sistemas web', 200, 1, 0,
 '2025-09-01', '2026-03-01', 'ENCERRADA', 2, 5, 1);


-- (Opcional - PostgreSQL) Reajustar sequences após inserts com ID explícito
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuario));
SELECT setval(pg_get_serial_sequence('curso', 'id_curso'), (SELECT MAX(id_curso) FROM curso));
SELECT setval(pg_get_serial_sequence('grupo', 'id_grupo'), (SELECT MAX(id_grupo) FROM grupo));
SELECT setval(pg_get_serial_sequence('oportunidade', 'id_oportunidade'), (SELECT MAX(id_oportunidade) FROM oportunidade));
SELECT setval(pg_get_serial_sequence('solicitacao', 'id'), (SELECT MAX(id) FROM solicitacao));