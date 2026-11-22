# 2. SQL Injection — Conceitos

## 2.1 Primeiro, uma revisão rápida de SQL

**SQL** (*Structured Query Language*) é a linguagem usada para conversar com bancos de dados relacionais.

Exemplo de tabela `users`:

| id | name | password | role |
|---:|---|---|---|
| 1 | admin | (hash) | ADMIN |
| 2 | maria | (hash) | USER |
| 3 | joao | (hash) | USER |

Comandos mais comuns:

```sql
SELECT * FROM users WHERE name = 'maria';       -- ler
INSERT INTO users (name, role) VALUES ('ana','USER');  -- criar
UPDATE users SET role = 'USER' WHERE id = 3;    -- alterar
DELETE FROM users WHERE id = 3;                 -- apagar
```

Dois detalhes de sintaxe que importam para este módulo:

- Texto vai entre **aspas simples**: `'maria'`
- `--` inicia um **comentário** (o banco ignora o resto da linha)

## 2.2 O que é SQL Injection?

**SQL Injection (SQLi)** é uma vulnerabilidade em que a aplicação permite que **dados enviados pelo usuário alterem a estrutura de uma consulta SQL**.

Em outras palavras: o usuário deveria fornecer apenas um **valor** (um nome, uma senha), mas consegue fornecer **parte do comando**.

### Analogia: o formulário do banco

Imagine um formulário de papel com a frase:

> "Eu, ____________, autorizo o saque de R$ 100."

Você deveria escrever só um nome. Mas se escrever:

> "Eu, **João. Também autorizo o saque de R$ 10.000 da conta do Pedro**, autorizo o saque de R$ 100."

O funcionário lê tudo como uma única instrução. O problema não está em quem escreveu: está em **quem leu sem separar o que é dado do que é ordem**.

```text
Dado   → "o que" (um nome, uma senha, um número)
Comando → "o que fazer" (SELECT, DELETE, ...)

SQL Injection = o dado é tratado como comando.
```

## 2.3 Como uma entrada do usuário chega ao banco de dados?

```text
┌─────────┐   1. digita     ┌────────────┐
│ Usuário │ ──────────────▶ │ Navegador  │
└─────────┘  usuário/senha  └─────┬──────┘
                                  │ 2. requisição HTTP (POST /login)
                                  ▼
                            ┌────────────┐
                            │ Aplicação  │  3. monta a consulta SQL
                            └─────┬──────┘     com o que recebeu
                                  │ 4. envia a consulta
                                  ▼
                            ┌────────────┐
                            │   Banco    │  5. executa e devolve
                            │  de Dados  │     o resultado
                            └─────┬──────┘
                                  │
                                  ▼
                       6. aplicação decide: "login ok" ou "falhou"
```

O passo **3** é onde mora o risco. Tudo o que vem do usuário é **não confiável**.

> **Regra de ouro:** toda entrada que vem do cliente (formulário, URL, cookie, header, JSON) deve ser tratada como potencialmente maliciosa.

## 2.4 Como consultas SQL são construídas de forma insegura?

A forma insegura é **concatenar texto**: juntar a entrada do usuário diretamente na string da consulta.

```java
String sql =
    "SELECT * FROM users WHERE name = '" + name + "'";
```

O que acontece com um valor normal, `name = maria`:

```sql
SELECT * FROM users WHERE name = 'maria'
```

Funciona como esperado. Agora, o que acontece se o valor contiver uma **aspa simples**?

A aspa **fecha** o texto antes da hora. O que vem depois deixa de ser "dado" e passa a ser interpretado como **SQL**.

```text
Consulta montada pela aplicação:

SELECT * FROM users WHERE name = ' <ENTRADA> '
                                   ▲
                                   └── aqui o usuário controla o conteúdo
                                       e pode "escapar" das aspas
```

### O exemplo clássico: tela de login

Consulta vulnerável:

```java
String sql = "SELECT * FROM users WHERE name = '" + name
           + "' AND password = '" + password + "'";
```

**Uso normal** (`name = maria`, `password = abc123`):

```sql
SELECT * FROM users WHERE name = 'maria' AND password = 'abc123'
```

→ Retorna a linha só se nome **e** senha baterem.

**Entrada manipulada** (`name = admin'--`, `password = qualquer`):

```sql
SELECT * FROM users WHERE name = 'admin'--' AND password = 'qualquer'
```

Repare:

1. `admin'` fecha o texto do nome.
2. `--` transforma **todo o resto** em comentário.
3. A verificação da senha **desapareceu** da consulta.

A consulta efetiva passa a ser apenas:

```sql
SELECT * FROM users WHERE name = 'admin'
```

→ Retorna o administrador, **sem precisar da senha**.

Outra forma conhecida é fazer a condição ficar sempre verdadeira (`' OR '1'='1`), que veremos na demonstração.

> A partir daqui, o aprendizado é entender **o mecanismo**: a aplicação não distingue o que é dado do que é comando. Esse é o ponto que a correção vai atacar.

## 2.5 Por que a vulnerabilidade acontece?

Por quatro causas combinadas:

| # | Causa | Explicação |
|---|---|---|
| 1 | **Mistura de dados e comandos** | A consulta é uma string única, e o banco não sabe o que veio do programador e o que veio do usuário |
| 2 | **Confiança na entrada** | O código assume que o usuário "vai digitar só o que se espera" |
| 3 | **Concatenação de strings** | Montar SQL com `+` é fácil, rápido e perigoso |
| 4 | **Falta de conhecimento / pressa** | O código "funciona nos testes normais", então ninguém desconfia |

```text
O banco recebe UMA string.
Ele não tem como saber onde termina o código do programador
e onde começa o texto do usuário.
```

## 2.6 Tipos de SQL Injection (visão geral)

Não é necessário dominar todos agora; o importante é saber que existem.

| Tipo | Como se manifesta |
|---|---|
| **In-band (clássico)** | O resultado aparece na própria resposta da aplicação |
| **Baseada em erro** | As mensagens de erro do banco revelam informações |
| **Union-based** | Combina o resultado da consulta original com outro |
| **Blind (cega)** | A aplicação não mostra o resultado, mas o comportamento muda (verdadeiro/falso, tempo de resposta) |
| **Second-order** | O dado malicioso é salvo e só causa efeito quando usado depois, em outra consulta |

No laboratório, o foco é o **login vulnerável** (in-band).

## 2.7 Impactos do SQL Injection

O impacto depende do que a aplicação faz e das permissões do usuário do banco, mas pode ser **muito grave**.

| Pilar da segurança | Impacto possível |
|---|---|
| **Confidencialidade** | Vazamento de dados de clientes, senhas, documentos |
| **Integridade** | Alteração ou exclusão de registros (preços, saldos, permissões) |
| **Disponibilidade** | Apagar tabelas, derrubar o sistema |
| **Autenticação** | Entrar como outro usuário, inclusive administrador |
| **Sistema** | Em configurações ruins, acesso ao servidor do banco ou além |

### Impacto no negócio

- Multas e processos (ex.: **LGPD** em caso de vazamento de dados pessoais)
- Perda de confiança e de clientes
- Custo de resposta a incidentes
- Interrupção de serviços

> SQL Injection é uma das vulnerabilidades mais antigas conhecidas, e ainda aparece em incidentes reais. Não por ser difícil de evitar, mas por ser fácil de esquecer.

## 2.8 Como evitar SQL Injection

Visão geral (detalhes e código no próximo arquivo):

| Prioridade | Medida | Por quê |
|:---:|---|---|
| 1 | **Consultas parametrizadas** (*Prepared Statements*) | Separa dados de comandos de forma estrutural |
| 2 | **ORMs / Query Builders** usados corretamente | Parametrizam por padrão (cuidado com SQL "cru") |
| 3 | **Validação de entrada** (tipo, tamanho, formato) | Reduz a superfície, mas **não substitui** o item 1 |
| 4 | **Privilégio mínimo** no usuário do banco | Limita o estrago se algo falhar |
| 5 | **Mensagens de erro genéricas** | Não revelar estrutura do banco |
| 6 | **Logs e monitoramento** | Detectar tentativas |
| 7 | **Senhas com hash + salt** | Mesmo com vazamento, as senhas não ficam expostas |

```text
Defesa principal:  separar DADOS de COMANDOS
Defesa em camadas: validação + privilégio mínimo + erros genéricos + logs
```

### Mitos comuns

| Mito | Realidade |
|---|---|
| "Basta trocar aspas por outro caractere" | Listas de caracteres proibidos são frágeis e contornáveis |
| "Validar no JavaScript do navegador resolve" | O atacante ignora o navegador e envia a requisição direto |
| "Meu site é pequeno, ninguém ataca" | Ataques automatizados varrem a internet inteira |
| "Usar framework já me protege" | Só se você **não** concatenar strings em consultas "cruas" |

## Fixando

1. Qual a diferença entre **dado** e **comando** em uma consulta SQL?
2. Por que concatenar strings para montar SQL é perigoso?
3. No exemplo `name = 'admin'--' AND password = ...`, o que o `--` faz?
4. Cite três impactos possíveis de um SQL Injection.
5. Por que validar apenas no navegador não protege?

<details>
<summary>Respostas</summary>

1. Dado é um valor (nome, senha). Comando é a instrução (SELECT, DELETE).
2. Porque o banco recebe uma string única e não distingue o que o programador escreveu do que o usuário digitou.
3. Comenta o resto da linha, removendo a verificação da senha.
4. Vazamento de dados, alteração/exclusão de dados, bypass de login, indisponibilidade.
5. Porque o atacante pode enviar a requisição diretamente ao servidor, sem passar pelo navegador.
</details>
