CREATE TABLE users (
    id       INT PRIMARY KEY,
    name     VARCHAR(50)  NOT NULL,
    password VARCHAR(100) NOT NULL,  -- texto puro APENAS porque é laboratório (hash: módulo 5)
    role     VARCHAR(20)  NOT NULL
);

CREATE TABLE products (
    id          INT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL
);
