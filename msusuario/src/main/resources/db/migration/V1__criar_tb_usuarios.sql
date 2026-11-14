CREATE TABLE tb_usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    matricula VARCHAR(40) NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_matricula UNIQUE (matricula)
);
