# 3. Demonstração prática — Login vulnerável

> **Ambiente:** somente o laboratório do curso (ou instalação local própria). Nunca teste em sistemas de terceiros.

## 3.1 Objetivo da demonstração

Mostrar, em uma tela de login, **a diferença entre o comportamento normal e o comportamento sob ataque**, e identificar exatamente onde e por que a falha acontece.

```text
Usuário
   ↓
Aplicação
   ↓
Consulta SQL
   ↓
Banco de Dados
   ↓
Resultado
```

## 3.2 Cenário

Tela de login da aplicação de laboratório:

```text
┌──────────────────────────────┐
│          LOGIN               │
│                              │
│  Usuário: [______________]   │
│  Senha:   [______________]   │
│                              │
│           [ Entrar ]         │
└──────────────────────────────┘
```

Dados no banco (senhas em texto puro **apenas porque é um laboratório**; veremos o certo no módulo 5):

| id | name | password | role |
|---:|---|---|---|
| 1 | admin | Adm!n#2024 | ADMIN |
| 2 | maria | abc123 | USER |
| 3 | joao | senha456 | USER |

Requisição enviada pelo navegador (visível nas DevTools, aba *Network*):

```http
POST /login HTTP/1.1
Host: laboratorio.local
Content-Type: application/x-www-form-urlencoded

username=maria&password=abc123
```

## 3.3 Parte A — Comportamento normal

Antes de testar a falha, **sempre entenda o comportamento esperado**. Um profissional compara "normal" com "anormal".

| Teste | Usuário | Senha | Resultado esperado |
|---|---|---|---|
| 1 | maria | abc123 | Login com sucesso |
| 2 | maria | errada | "Usuário ou senha inválidos" |
| 3 | inexistente | qualquer | "Usuário ou senha inválidos" |

### O que acontece por trás no teste 1

A aplicação monta:

```sql
SELECT * FROM users WHERE name = 'maria' AND password = 'abc123'
```

O banco devolve **1 linha** → a aplicação conclui: "autenticado".

### No teste 2

```sql
SELECT * FROM users WHERE name = 'maria' AND password = 'errada'
```

O banco devolve **0 linhas** → a aplicação conclui: "falhou".

> A lógica da aplicação é: **se a consulta retornou alguma linha, o login é válido.** Guarde essa frase, ela é a chave da vulnerabilidade.

## 3.4 Parte B — Demonstrando a vulnerabilidade

### Teste 1 — Observar a reação a uma aspa simples

Digite no campo **Usuário**: `'` (uma única aspa) e qualquer senha.

**Hipótese:** se a aplicação concatena a entrada, a aspa vai quebrar a sintaxe da consulta:

```sql
SELECT * FROM users WHERE name = ''' AND password = 'x'
```

**Resultado possível:** erro 500, página de erro ou mensagem de erro do banco.

**Por que isso é um sinal importante?** Uma aspa não deveria causar erro em um sistema que trata dados corretamente. O erro indica que a entrada **está sendo interpretada como parte do SQL**. É um indício forte (não uma prova) de SQL Injection.

### Teste 2 — Contornar a verificação de senha

Campo **Usuário:** `admin'--`
Campo **Senha:** `qualquer`

Consulta resultante:

```sql
SELECT * FROM users WHERE name = 'admin'--' AND password = 'qualquer'
```

O que o banco "enxerga":

```text
SELECT * FROM users WHERE name = 'admin'   ← executado
--' AND password = 'qualquer'              ← comentário (ignorado)
```

**Resultado:** o banco devolve a linha do `admin`. A aplicação aplica sua regra ("veio linha → login válido") e **autentica como administrador, sem saber a senha**.

### Teste 3 — Condição sempre verdadeira

Campo **Usuário:** `x`
Campo **Senha:** `' OR '1'='1`

Consulta resultante:

```sql
SELECT * FROM users WHERE name = 'x' AND password = '' OR '1'='1'
```

Como `AND` tem precedência sobre `OR`, a leitura é:

```text
(name = 'x' AND password = '')   OR   ('1' = '1')
        falso                           verdadeiro
                    └──────── resultado: verdadeiro ────────┘
```

A condição vale para **todas as linhas**: o banco devolve a tabela inteira, e a aplicação costuma pegar a primeira (geralmente o admin).

### Resumo visual: normal x ataque

```text
NORMAL
  entrada:  maria / abc123
  SQL:      ... WHERE name = 'maria' AND password = 'abc123'
  banco:    1 linha  → login de maria

ATAQUE (bypass de autenticação)
  entrada:  admin'-- / qualquer
  SQL:      ... WHERE name = 'admin'--' AND password = '...'
                                      └─ ignorado
  banco:    1 linha  → login de admin (sem senha!)
```

## 3.5 Parte C — Identificando o impacto

Pergunte-se: **o que um atacante conseguiu com essa falha?**

| Observação | Impacto |
|---|---|
| Entrou como `admin` sem a senha | Quebra de **autenticação** |
| O admin vê dados de todos os usuários | Quebra de **confidencialidade** |
| O admin pode editar/excluir registros | Quebra de **integridade** |
| Tudo isso a partir de **um campo de texto** | Baixo custo, alto impacto |

Registre a evidência como um profissional faria:

```text
Título:       SQL Injection na tela de login
Local:        POST /login, parâmetro "username"
Categoria:    OWASP Top 10 — Injection
Evidência:    entrada  admin'--  autenticou como ADMIN sem senha
Impacto:      Acesso administrativo não autorizado
Severidade:   Crítica
Correção:     Consultas parametrizadas (PreparedStatement)
```

## 3.6 Como o aluno enxerga isso no navegador

1. Abra as **DevTools** (F12) → aba **Network**.
2. Marque *Preserve log*.
3. Faça o login normal e localize a requisição `login`.
4. Observe: método, URL, headers, body, status e resposta.
5. Repita com a entrada manipulada e **compare as respostas** (status, tamanho, redirecionamento, cookies).

Perguntas para observar:

- O **status code** mudou? (200, 302, 401, 500?)
- Houve **redirecionamento** para uma área restrita?
- Um **cookie de sessão** foi criado?
- A resposta contém **mensagens de erro** do banco?

## 3.7 Pontos de atenção na demonstração

- O objetivo é **entender e corrigir**, não "invadir".
- Mensagens de erro detalhadas ajudam o atacante: anote isso como problema separado (*configuração incorreta*).
- A falha **não está no campo de login**, está no **código que monta a consulta**. É isso que veremos a seguir.

## Fixando

1. Por que a aspa simples é um bom "teste de sondagem"?
2. No bypass com `admin'--`, por que a senha deixa de ser verificada?
3. Qual regra da aplicação ("se veio linha, login válido") torna o ataque eficaz?
4. Que informações compõem um bom registro de vulnerabilidade?

<details>
<summary>Respostas</summary>

1. Porque ela delimita texto em SQL; se causa erro, a entrada provavelmente está sendo interpretada como SQL.
2. Porque `--` comenta o restante da consulta, incluindo a condição da senha.
3. A aplicação confia que qualquer linha retornada significa credenciais válidas.
4. Título, local, categoria, evidência, impacto, severidade e correção sugerida.
</details>
