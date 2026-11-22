# 4. Análise do código, correção e reteste

## 4.1 Onde está a vulnerabilidade?

Voltamos ao código por trás da tela de login.

### Código vulnerável

```java
public boolean login(String name, String password) throws SQLException {

    String sql = "SELECT * FROM users WHERE name = '" + name
               + "' AND password = '" + password + "'";      // (1)

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(sql);                    // (2)

    return rs.next();                                         // (3)
}
```

### Lendo linha a linha

| Marca | O que acontece | Problema |
|:---:|---|---|
| **(1)** | A entrada do usuário (`name`, `password`) é **concatenada** na string SQL | Dados e comandos se misturam |
| **(2)** | `Statement` envia a string pronta ao banco | O banco interpreta tudo como SQL |
| **(3)** | `rs.next()` é `true` se veio **qualquer** linha | Basta a consulta retornar algo para "logar" |

> **A causa raiz é a linha (1).** Todo o resto é consequência.

### Como identificar esse padrão em qualquer código

Procure por **SQL montado com concatenação ou formatação de strings**:

```java
// Java
"SELECT ... " + variavel
String.format("SELECT ... %s", variavel)
```

```python
# Python
f"SELECT ... {variavel}"
"SELECT ... " + variavel
"SELECT ... %s" % variavel      # formatação de string, NÃO parametrização
```

```javascript
// JavaScript / Node
`SELECT ... ${variavel}`
"SELECT ... " + variavel
```

```php
// PHP
"SELECT ... $variavel"
```

**Sinal de alerta:** existe uma variável vinda do usuário dentro de uma string que contém `SELECT`, `INSERT`, `UPDATE` ou `DELETE`.

## 4.2 A correção: consultas parametrizadas

Em vez de montar o SQL com os valores, a aplicação envia **duas coisas separadas** ao banco:

1. **O modelo da consulta**, com marcadores (`?`) no lugar dos valores.
2. **Os valores**, em um canal à parte.

```text
ANTES (inseguro)
  [ "SELECT ... name = '" + name + "'" ]  ──▶  uma string só  ──▶  banco interpreta tudo

DEPOIS (seguro)
  [ "SELECT ... name = ?" ]  ──▶  banco compila a estrutura primeiro
  [ name = valor do usuário ] ──▶  entra depois, apenas como DADO
```

### Código corrigido

```java
public boolean login(String name, String password) throws SQLException {

    String sql = "SELECT * FROM users WHERE name = ? AND password = ?";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, name);        // 1º "?" recebe name
        stmt.setString(2, password);    // 2º "?" recebe password

        try (ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        }
    }
}
```

### Por que isso resolve?

O banco **já conhece a estrutura da consulta** antes de receber os valores. O que o usuário digitar será sempre tratado como **texto literal**, nunca como parte do comando.

Veja o que ocorre com a entrada maliciosa `admin'--`:

```text
Antes (concatenação):
  name = 'admin'--' AND ...   → o "--" vira comentário  ❌

Depois (parametrizado):
  name = ?   com   ? = "admin'--"
  → o banco procura um usuário chamado literalmente  admin'--
  → não existe → 0 linhas → login negado  ✅
```

> **Ideia-chave:** a segurança vem da **estrutura**, não de "limpar" a entrada. Por isso é a defesa principal.

## 4.3 Atenção: o que NÃO é consulta parametrizada

```java
// ❌ continua vulnerável: PreparedStatement com concatenação
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM users WHERE name = '" + name + "'"
);
```

Usar a classe `PreparedStatement` **não basta**. O que protege é usar `?` e passar valores via `setXxx`.

```python
# ❌ vulnerável (formatação de string)
cursor.execute(f"SELECT * FROM users WHERE name = '{name}'")

# ✅ seguro (parâmetro)
cursor.execute("SELECT * FROM users WHERE name = %s", (name,))
```

```javascript
// ❌ vulnerável
db.query(`SELECT * FROM users WHERE name = '${name}'`);

// ✅ seguro
db.query("SELECT * FROM users WHERE name = ?", [name]);
```

### Limitação importante

Parâmetros servem para **valores**. Eles **não** funcionam para nomes de tabelas, colunas ou direção de ordenação (`ORDER BY`). Nesses casos, use uma **lista de valores permitidos** (*allowlist*):

```java
// Exemplo: ordenação escolhida pelo usuário
Set<String> permitidos = Set.of("name", "created_at");
String coluna = permitidos.contains(campo) ? campo : "name";
String sql = "SELECT * FROM users ORDER BY " + coluna;   // valor vem da allowlist, não do usuário
```

## 4.4 Segunda camada: o hash de senha muda o design

No exemplo de laboratório a senha está em texto puro, o que **não deve ser feito** (módulo 5). Em um sistema real:

1. Busca-se o usuário **somente pelo nome**.
2. Compara-se o **hash** da senha informada com o hash armazenado.

```java
String sql = "SELECT password_hash FROM users WHERE name = ?";

try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    stmt.setString(1, name);
    try (ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) return false;                 // usuário não existe
        String hash = rs.getString("password_hash");
        return passwordEncoder.matches(password, hash); // compara com segurança
    }
}
```

Assim, a **senha nunca entra na consulta SQL**, o que elimina mais um ponto de risco.

## 4.5 Defesa em camadas

A parametrização é a defesa principal, mas segurança boa tem **mais de uma barreira**:

```text
Camada 1: Consultas parametrizadas        ← impede a injeção
Camada 2: Validação de entrada            ← rejeita o que não faz sentido
Camada 3: Privilégio mínimo no banco      ← limita o estrago
Camada 4: Erros genéricos                 ← não ajuda quem investiga
Camada 5: Logs e alertas                  ← detecta tentativas
```

### Privilégio mínimo

O usuário do banco usado pela aplicação **não deve ser administrador**.

| Usuário do banco | Permissões |
|---|---|
| ❌ `root` / `sa` / `postgres` | Tudo, inclusive apagar bancos |
| ✅ `app_user` | Apenas `SELECT/INSERT/UPDATE` nas tabelas necessárias |

### Mensagens de erro genéricas

```text
❌  ERROR: syntax error at or near "'" in: SELECT * FROM users WHERE name = '''
✅  Ocorreu um erro. Tente novamente. (detalhes registrados apenas no log do servidor)
```

### Validação de entrada

Complementa, mas **não substitui**:

```java
if (name == null || !name.matches("[a-zA-Z0-9_.-]{3,30}")) {
    return false;    // rejeita formato inesperado
}
```

> Cuidado: validar com lista de caracteres permitidos ajuda, mas senhas e textos livres precisam aceitar símbolos. Por isso a parametrização é obrigatória.

## 4.6 Reteste: provando que a correção funcionou

**Corrigir sem retestar é apenas esperança.** O reteste fecha o ciclo.

### Checklist do reteste

| # | Teste | Antes da correção | Depois da correção |
|---|---|---|---|
| 1 | Login normal (`maria` / `abc123`) | Sucesso | **Sucesso** (a funcionalidade continua funcionando) |
| 2 | Senha errada | Falha | **Falha** |
| 3 | Aspa simples no usuário (`'`) | Erro 500 | **Mensagem normal de login inválido** |
| 4 | `admin'--` no usuário | Entrou como admin | **Login negado** |
| 5 | `' OR '1'='1` na senha | Entrou | **Login negado** |

### O que observar

- A **funcionalidade legítima** continua funcionando?
- As entradas manipuladas agora são **tratadas como texto comum**?
- As respostas não revelam detalhes internos?
- O **log** registrou algo suspeito?

### Boa prática: teste automatizado

Transforme o reteste em um teste que roda sempre, para a falha **não voltar** no futuro:

```java
@Test
void loginNaoDeveSerVulneravelASqlInjection() throws Exception {
    assertFalse(authService.login("admin'--", "qualquer"));
    assertFalse(authService.login("x", "' OR '1'='1"));
}

@Test
void loginLegitimoDeveFuncionar() throws Exception {
    assertTrue(authService.login("maria", "abc123"));
}
```

## 4.7 O ciclo completo

```text
Encontrar   → aspa simples gerou erro
   ↓
Entender    → concatenação mistura dado e comando
   ↓
Testar      → admin'-- autentica sem senha
   ↓
Impacto     → acesso administrativo não autorizado
   ↓
Corrigir    → PreparedStatement com parâmetros
   ↓
Testar de novo → ataque falha, login legítimo funciona
```

## Fixando

1. Qual linha do código vulnerável é a causa raiz?
2. Por que `PreparedStatement` com concatenação continua vulnerável?
3. Parâmetros funcionam para nome de coluna? O que usar então?
4. Por que o reteste deve incluir o login legítimo?
5. Dê dois exemplos de camadas de defesa além da parametrização.

<details>
<summary>Respostas</summary>

1. A que concatena `name` e `password` na string SQL.
2. Porque o problema é a entrada fazer parte da string da consulta; a classe sozinha não separa dados de comandos.
3. Não. Use uma lista de valores permitidos (allowlist).
4. Para garantir que a correção não quebrou o funcionamento normal.
5. Privilégio mínimo no banco, erros genéricos, validação de entrada, logs e alertas.
</details>
