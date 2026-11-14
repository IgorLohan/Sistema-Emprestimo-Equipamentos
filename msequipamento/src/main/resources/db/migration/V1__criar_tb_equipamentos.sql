CREATE TABLE tb_equipamentos (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    patrimonio VARCHAR(60) NOT NULL,
    disponivel BOOLEAN NOT NULL,
    reserva_emprestimo_id UUID,
    CONSTRAINT uk_equipamentos_patrimonio UNIQUE (patrimonio)
);
