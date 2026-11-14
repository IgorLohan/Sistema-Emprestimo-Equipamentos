CREATE TABLE tb_emprestimos (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    equipamento_id UUID NOT NULL,
    data_emprestimo TIMESTAMP NOT NULL,
    data_devolucao TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_emprestimos_status CHECK (status IN ('ATIVO', 'DEVOLVIDO', 'CANCELADO'))
);

CREATE INDEX idx_emprestimos_usuario_id ON tb_emprestimos (usuario_id);

CREATE UNIQUE INDEX uk_emprestimo_equipamento_ativo
    ON tb_emprestimos (equipamento_id)
    WHERE status = 'ATIVO';
