INSERT INTO setores (nome, sigla) VALUES
    ('Coordenação de Tecnologia da Informação e Comunicação', 'COTIC'),
    ('Manutenção', 'MANUT');

INSERT INTO categorias (nome, descricao, setor_id) VALUES
    ('Redes e Internet', 'Problemas de conectividade e acesso à rede', 1),
    ('Equipamentos de TI', 'Computadores, projetores e periféricos', 1),
    ('Infraestrutura', 'Reparos em salas e instalações físicas', 2),
    ('Instalações Elétricas', 'Tomadas, iluminação e circuitos elétricos', 2);

INSERT INTO usuarios (nome, email, matricula, tipo, setor_id) VALUES
    ('Ana Silva', 'ana@ifrn.edu.br', '20260001', 'SOLICITANTE', NULL),
    ('Bruno Souza', 'bruno@ifrn.edu.br', '20260002', 'SOLICITANTE', NULL),
    ('Carlos Lima', 'carlos@ifrn.edu.br', '20260003', 'TECNICO', 1),
    ('Daniela Santos', 'daniela@ifrn.edu.br', '20260004', 'TECNICO', 2);
