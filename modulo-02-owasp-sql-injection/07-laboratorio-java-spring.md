# 7. Laboratório prático em Java + Spring Boot: SQL Injection na prática

> **Aviso:** o projeto [`laboratorio-sqli/`](laboratorio-sqli) contém uma API **propositalmente vulnerável**. Rode somente na sua máquina, nunca exponha em rede ou Internet, e nunca use essas técnicas em sistemas de terceiros sem autorização.

## 7.1 O que é o projeto

Uma única aplicação Spring Boot com **duas versões da mesma API**, para comparar lado a lado:

| | ❌ `/vulneravel/**` | ✅ `/seguro/**` |
|---|---|---|
| Montagem do SQL | Concatenação de strings | Consulta parametrizada (`?`) |
| `ORDER BY` | Valor do usuário direto no SQL | Allowlist de colunas |
| Validação de entrada | Nenhuma | Tamanho e nulidade |
| Erros | Devolve SQL e erro do banco | Mensagem genérica (detalhe só no log) |

Endpoints (iguais nas duas versões):

| Método | Rota | Parâmetros |
|---|---|---|
| `POST` | `/{versao}/login` | JSON `{"username": "...", "password": "..."}` |
| `GET` | `/{versao}/produtos` | `busca` (texto), `ordenar` (coluna) |

Stack: **Java 25 (LTS)**, **Spring Boot 4.2**, **Spring JDBC (`JdbcClient`)** e **H2 em memória** (recriado a cada execução, então nada que você "estragar" persiste).

> **Sobre o Java 27:** ele ainda não foi lançado (previsto para setembro de 2027). O projeto usa o Java 25, a LTS atual. Para trocar no futuro, basta alterar `<java.version>` no `pom.xml`.

### Dados do banco (criados em `schema.sql` e `data.sql`)

`users`:

| id | name | password | role |
|---:|---|---|---|
| 1 | admin | Adm!n#2024 | ADMIN |
| 2 | maria | abc123 | USER |
| 3 | joao | senha456 | USER |

`products`: Notebook, Mouse, Teclado, Monitor (colunas `id`, `name`, `description`).

## 7.2 Como executar

Pré-requisitos: **JDK 25** e **Maven 3.9+**.

```bash
cd modulo-02-owasp-sql-injection/laboratorio-sqli

mvn spring-boot:run      # sobe em http://localhost:8080
mvn test                 # roda a bateria de ataques contra as duas versões
```

### Interface visual

Com a aplicação rodando, abra **http://localhost:8080** no navegador. A página (`src/main/resources/static/index.html`) permite:

1. escolher o alvo: ❌ vulnerável, ✅ segura ou ⚖️ **comparar** (envia a mesma requisição às duas e mostra lado a lado);
2. montar a requisição de login (`POST`) ou de produtos (`GET`) editando os campos;
3. clicar em um **payload pronto** (ataques 1 a 5 e usos normais) que preenche os campos;
4. ver o status HTTP, o **SQL que chegou ao banco**, o erro vazado, as linhas retornadas em tabela e a resposta bruta.

O arquivo [`requests.http`](laboratorio-sqli/requests.http) tem todas as requisições deste documento prontas para IntelliJ ou VS Code (extensão REST Client). Os exemplos abaixo usam `curl`.

## 7.3 Por que a versão vulnerável é vulnerável

Em [`VulnerableRepository.java`](laboratorio-sqli/src/main/java/br/com/curso/sqli/vulneravel/VulnerableRepository.java):

```java
return "SELECT id, name, role FROM users"
        + " WHERE name = '" + username + "'"
        + " AND password = '" + password + "'";
```

O código **cola o texto do usuário dentro do comando SQL**. O banco recebe uma string única e não tem como saber o que era "estrutura" (escrita pelo programador) e o que era "dado" (digitado pelo usuário). Se o usuário digitar aspas e palavras-chave SQL, ele **muda a estrutura da consulta**.

> Regra mental: **entrada do usuário nunca pode virar código**.

## 7.4 Como o ataque funciona (passo a passo)

A API vulnerável devolve o campo `sqlExecutado` justamente para você ver o que o banco recebeu. (Em um sistema real o atacante não veria isso, mas deduziria a partir dos erros e do comportamento.)

### Ataque 1: Sondagem com aspa simples

Procure um sinal de que a entrada é interpretada como SQL.

```bash
curl -s -X POST localhost:8080/vulneravel/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"'"'"'","password":"x"}'
```

SQL montado: `... WHERE name = ''' AND password = 'x'`. A aspa abre/fecha texto de forma desbalanceada e a resposta é **HTTP 500 com erro de sintaxe do banco**. Uma aspa não deveria causar erro, então isso indica que a entrada virou SQL.

### Ataque 2: Bypass de autenticação com comentário (`--`)

Usuário: `admin'--`  |  Senha: qualquer coisa.

```bash
curl -s -X POST localhost:8080/vulneravel/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin'"'"'--","password":"qualquer"}'
```

SQL montado:

```sql
SELECT id, name, role FROM users WHERE name = 'admin'--' AND password = 'qualquer'
--                                              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ comentário
```

A aspa depois de `admin` fecha o texto e o `--` comenta o resto, **eliminando a verificação de senha**. Resultado: login como `ADMIN` sem saber a senha.

### Ataque 3: Condição sempre verdadeira (`OR '1'='1`)

Usuário: `x`  |  Senha: `' OR '1'='1`

```bash
curl -s -X POST localhost:8080/vulneravel/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"x","password":"'"'"' OR '"'"'1'"'"'='"'"'1"}'
```

SQL montado:

```sql
SELECT id, name, role FROM users WHERE name = 'x' AND password = '' OR '1'='1'
```

Como `AND` tem precedência sobre `OR`: `(name='x' AND password='') OR ('1'='1')`. O lado direito é sempre verdadeiro, então **todas as linhas** são devolvidas e a aplicação autentica a primeira (o admin).

### Ataque 4: `UNION`, extraindo dados de outra tabela

A busca de produtos não tem relação com a tabela `users`, mas a injeção permite **anexar uma segunda consulta**:

```bash
curl -s -G localhost:8080/vulneravel/produtos \
  --data-urlencode "busca=zzz' UNION SELECT id, name, password FROM users --"
```

SQL montado:

```sql
SELECT id, name, description FROM products WHERE name LIKE '%zzz'
UNION SELECT id, name, password FROM users --%'
```

- `zzz` não casa com nenhum produto, então a primeira parte retorna vazio.
- `UNION` junta o resultado de `SELECT ... FROM users`.
- `--` comenta o `%'` que sobrou.

A resposta lista **usuário e senha de todo mundo** dentro do campo `description`. O fluxo de um atacante real:

1. **Descobrir** que a entrada é injetável (aspa causa erro).
2. **Contar colunas**: o `UNION` exige o mesmo número de colunas. Tenta-se `' ORDER BY 1 --`, `ORDER BY 2 --`, `ORDER BY 3 --`, `ORDER BY 4 --`; o erro surge no 4, então são 3 colunas.
3. **Achar colunas compatíveis em tipo** (texto com texto, número com número).
4. **Descobrir nomes de tabelas** via catálogo do banco (no H2: `INFORMATION_SCHEMA.TABLES`).
5. **Extrair os dados**.

Veja o passo 4 no laboratório:

```bash
curl -s -G localhost:8080/vulneravel/produtos \
  --data-urlencode "busca=zzz' UNION SELECT 1, table_name, column_name FROM information_schema.columns WHERE table_schema='PUBLIC' --"
```

### Ataque 5: Injeção no `ORDER BY` (blind)

O parâmetro `ordenar` também é concatenado. Aqui o atacante não vê dados diretamente; ele **faz perguntas de sim/não** ao banco e observa a ordem do resultado:

```bash
# Pergunta: "existe usuário admin?" -> se sim, ordem invertida (id decrescente)
curl -s -G localhost:8080/vulneravel/produtos \
  --data-urlencode "ordenar=CASE WHEN (SELECT COUNT(*) FROM users WHERE name='admin')>0 THEN -id ELSE id END"
```

Repetindo com perguntas como `SUBSTRING(password,1,1)='A'`, ele reconstrói informação **caractere por caractere**. É a técnica *blind SQL injection*: lenta, mas automatizável (ferramentas como o `sqlmap` fazem isso).

### Ataque 6: Alteração de dados (stacked queries)

Alguns bancos/drivers aceitam vários comandos separados por `;`. O H2 aceita, então um ponto de injeção em um `INSERT`/`UPDATE`/`DELETE` permitiria algo como `'; DELETE FROM users; --`. Nesta API só há `SELECT`, mas o princípio é o mesmo: **quem injeta pode fazer tudo o que o usuário do banco tem permissão de fazer**. Por isso existe o princípio do privilégio mínimo (seção 7.5).

### Resumo do impacto

| Ataque | Propriedade violada |
|---|---|
| Bypass de login | Autenticação |
| `UNION` / blind | Confidencialidade |
| Stacked queries | Integridade e disponibilidade |

## 7.5 Como se proteger: a versão segura

### Defesa principal: consultas parametrizadas

Em [`SecureRepository.java`](laboratorio-sqli/src/main/java/br/com/curso/sqli/seguro/SecureRepository.java):

```java
jdbc.sql("SELECT id, name, role FROM users WHERE name = ? AND password = ?")
    .param(username)
    .param(password)
    .query(...)
```

**Por que isso resolve:** o SQL é **fixo** e enviado ao banco com marcadores `?`. O banco analisa e "compila" a estrutura **antes** de receber os valores. Depois os valores chegam por um canal separado e são sempre tratados como **dado literal**. O texto `admin'--` vira apenas um nome de usuário esquisito que não existe.

```text
VULNERÁVEL:  [SQL + entrada] ──► banco interpreta TUDO como SQL
SEGURO:      [SQL com ?] ──► banco entende a estrutura
             [valores]   ──► banco usa como dados, nunca como SQL
```

Logs do Spring confirmam (rode e observe o console):

```text
vulnerável: SELECT ... WHERE name = 'admin'--' AND password = 'x'
seguro:     SELECT ... WHERE name = ? AND password = ?
```

### O curinga do `LIKE` faz parte do valor

```java
.param("%" + busca + "%")   // ✅ certo: o % é concatenado ao VALOR
```

e **não** `"... LIKE '%?%'"`: dentro das aspas o `?` seria texto, não parâmetro.

### Quando não dá para usar `?`: allowlist

Nome de coluna, tabela e direção (`ASC`/`DESC`) **não podem ser parâmetros**. A solução é mapear a escolha do usuário para um trecho de SQL que **você** escreveu:

```java
private static final Map<String, String> ORDENACOES = Map.of(
        "id", "id", "nome", "name", "descricao", "description");

String coluna = ORDENACOES.getOrDefault(ordenar, "id");   // valor desconhecido => padrão seguro
```

O valor do usuário é só uma **chave de consulta**; nunca entra no SQL. O payload do ataque 5 simplesmente não existe no mapa e cai no padrão `id`.

### Armadilhas: o que NÃO é consulta parametrizada

```java
// ❌ concatenação "disfarçada"
jdbc.sql("SELECT ... WHERE name = '" + nome + "'");
jdbc.sql(String.format("SELECT ... WHERE name = '%s'", nome));
jdbc.sql("SELECT ... WHERE name = '" + nome.replace("'", "''") + "'");  // escape manual: frágil

// ✅ parâmetros
jdbc.sql("SELECT ... WHERE name = ?").param(nome);
jdbc.sql("SELECT ... WHERE name = :nome").param("nome", nome);   // nomeado, também seguro
```

Também vale para JPA/Hibernate: `@Query("... WHERE u.name = '" + nome + "'")` ou `createQuery("..." + nome)` são vulneráveis; `:nome` + `setParameter` ou *derived queries* (`findByName`) são seguros.

### Defesa em camadas (além da parametrização)

| Camada | No projeto | Por quê |
|---|---|---|
| Validação de entrada | `invalido(...)` limita tamanho e nulos | Reduz superfície; **não substitui** parametrização |
| Erros genéricos | `erroGenerico()` + `log.error` | Não ajuda o atacante a mapear o banco |
| Privilégio mínimo | (recomendação) | Usuário do banco da aplicação só com `SELECT`/`INSERT`/... nas tabelas que precisa, nunca `DROP` ou acesso a `INFORMATION_SCHEMA` sensível |
| Hash de senha | (módulo 5) | Mesmo vazando a tabela, as senhas não ficam legíveis. **Aqui estão em texto puro só por ser laboratório** |
| Testes automatizados | `SqlInjectionTest` | Garante que a correção não regride |

## 7.6 Reteste: provando que a correção funcionou

Rode os **mesmos ataques** contra `/seguro`:

```bash
# Ataque 2 (bypass)  -> 401 "Usuário ou senha inválidos"
curl -s -X POST localhost:8080/seguro/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin'"'"'--","password":"qualquer"}'

# Ataque 4 (UNION)   -> {"resultado":[]}
curl -s -G localhost:8080/seguro/produtos \
  --data-urlencode "busca=zzz' UNION SELECT id, name, password FROM users --"

# Ataque 5 (ORDER BY) -> ordem normal por id, payload ignorado
curl -s -G localhost:8080/seguro/produtos \
  --data-urlencode "ordenar=CASE WHEN (SELECT COUNT(*) FROM users WHERE name='admin')>0 THEN -id ELSE id END"
```

E confirme que o **uso legítimo continua funcionando** (um reteste verifica as duas coisas):

```bash
curl -s -X POST localhost:8080/seguro/login -H 'Content-Type: application/json' \
  -d '{"username":"maria","password":"abc123"}'          # 200 OK
curl -s -G localhost:8080/seguro/produtos --data-urlencode "busca=Mouse"
```

Tudo isso está automatizado em [`SqlInjectionTest.java`](laboratorio-sqli/src/test/java/br/com/curso/sqli/SqlInjectionTest.java) (`mvn test`): na classe `Vulneravel` os ataques **têm sucesso**; na classe `Seguro` os mesmos ataques **falham**.

| Cenário | `/vulneravel` | `/seguro` |
|---|---|---|
| `maria` / `abc123` | 200 | 200 |
| `'` | **500** (erro do banco) | 401 |
| `admin'--` | **200 (admin)** | 401 |
| `' OR '1'='1` | **200** | 401 |
| `UNION` na busca | **vaza usuários e senhas** | `[]` |
| `ORDER BY` malicioso | **executa subconsulta** | ignorado |

## 7.7 Atividades sugeridas

1. Suba a aplicação e reproduza os ataques 1 a 5 contra `/vulneravel`, anotando o `sqlExecutado` de cada um.
2. Descubra, usando só `UNION` e `information_schema`, quais tabelas e colunas existem (ataque 4).
3. Repita tudo em `/seguro` e explique, com os logs do console, por que falhou.
4. **Desafio:** crie um endpoint `/seguro/usuarios?role=USER` parametrizado e outro vulnerável equivalente. Escreva os testes para ambos.
5. **Desafio:** adicione um parâmetro `direcao` (`asc`/`desc`) à ordenação segura. Como garantir que só esses dois valores entrem no SQL?
6. **Reflexão:** por que *escapar aspas manualmente* não é uma defesa confiável? (Pense em outros tipos de dado, codificações e esquecimentos.)
