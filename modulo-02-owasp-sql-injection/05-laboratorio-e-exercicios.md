# 5. Laboratório e exercícios

> **Lembrete:** execute estes exercícios **apenas** na aplicação de laboratório do curso ou em uma cópia local sua. Não aplique em sistemas de terceiros.

## 5.1 Laboratório guiado — o ciclo completo

```text
Encontrar → Entender → Testar → Identificar o impacto → Corrigir → Testar novamente
```

Preencha o **relatório do laboratório** (modelo no final) conforme avança.

### Etapa 1 — Encontrar

1. Acesse a aplicação de laboratório.
2. Abra as DevTools (F12) → aba **Network**.
3. Navegue até a tela de login e faça um login legítimo.
4. Anote a requisição: método, URL, parâmetros, status.

**Pergunta:** quais campos o usuário controla? Qualquer um deles é um **ponto de entrada**.

### Etapa 2 — Entender

1. Escreva, com suas palavras, qual consulta SQL você acha que a aplicação executa.
2. Desenhe o fluxo `Usuário → Aplicação → Consulta SQL → Banco → Resultado`.
3. Responda: o que a aplicação considera "login válido"?

### Etapa 3 — Testar

1. Faça os **testes normais** (login correto, senha errada, usuário inexistente) e registre as respostas.
2. Teste uma **aspa simples** no usuário e compare com o comportamento normal.
3. Teste a entrada de bypass apresentada na aula (`admin'--`).
4. Registre status, redirecionamento, cookies e mensagens.

### Etapa 4 — Identificar o impacto

Responda:

- Com quem você autenticou?
- O que essa conta consegue ver ou fazer?
- Se isso acontecesse em produção, que dados estariam em risco?

### Etapa 5 — Corrigir

1. Localize no código-fonte o trecho que monta a consulta.
2. Substitua a concatenação por `PreparedStatement` com `?`.
3. Reinicie a aplicação.

### Etapa 6 — Testar novamente

1. Repita **todos** os testes da etapa 3.
2. Confirme: o login legítimo funciona e o ataque falha.
3. Compare "antes" e "depois" na tabela do relatório.

## 5.2 Exercícios

### Nível 1 — Conceitos

**1.** Marque **V** ou **F**:

| Afirmação | V/F |
|---|:---:|
| O OWASP Top 10 é uma lista de exatamente 10 vulnerabilidades específicas | |
| SQL Injection acontece quando dados do usuário alteram a estrutura da consulta | |
| Validar no JavaScript do navegador elimina o risco de SQL Injection | |
| Consultas parametrizadas separam dados de comandos | |
| Basta usar a classe `PreparedStatement` para estar protegido, mesmo concatenando | |

**2.** Complete: "SQL Injection acontece porque a aplicação mistura ________ com ________."

**3.** Cite três impactos possíveis de um SQL Injection em um sistema de e-commerce.

### Nível 2 — Análise de código

**4.** Identifique quais dos trechos abaixo são vulneráveis e **justifique**:

```java
// A
String sql = "SELECT * FROM produtos WHERE id = " + id;
```

```java
// B
PreparedStatement ps = conn.prepareStatement("SELECT * FROM produtos WHERE id = ?");
ps.setInt(1, id);
```

```python
# C
cursor.execute(f"SELECT * FROM clientes WHERE email = '{email}'")
```

```python
# D
cursor.execute("SELECT * FROM clientes WHERE email = %s", (email,))
```

```javascript
// E
db.query("SELECT * FROM pedidos WHERE cliente = " + req.query.cliente);
```

**5.** O código abaixo usa `PreparedStatement`. Ele é seguro? Explique.

```java
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = '" + name + "'"
);
```

**6.** Reescreva o trecho A do exercício 4 de forma segura.

### Nível 3 — Aplicação

**7.** Uma busca de produtos usa:

```java
String sql = "SELECT * FROM produtos WHERE nome LIKE '%" + termo + "%'";
```

a) Por que é vulnerável?
b) Reescreva com parâmetro. *(Dica: o `%` deve ser concatenado ao **valor**, não ao SQL.)*

**8.** Uma tela permite ordenar a lista por coluna escolhida pelo usuário (`?ordenar=preco`). Parâmetros `?` funcionam para isso? Proponha uma solução segura.

**9.** Monte o **registro de vulnerabilidade** para o login do laboratório (título, local, categoria OWASP, evidência, impacto, severidade, correção).

**10.** Escreva dois testes automatizados: um que verifique que a injeção falha e outro que verifique que o login legítimo funciona.

### Desafio de reflexão

**11.** Mesmo com a correção aplicada, cite **três medidas adicionais** que reduziriam o impacto de uma falha futura (pense em banco, erros, logs e senhas).

## 5.3 Gabarito

<details>
<summary>Ver respostas</summary>

**1.** F, V, F, V, F.

**2.** "dados" com "comandos".

**3.** Exemplos: vazamento de dados de clientes e cartões; alteração de preços ou pedidos; acesso a contas administrativas; exclusão de tabelas; indisponibilidade da loja.

**4.** A: vulnerável (concatenação, ainda que `id` seja número, se vier como texto do usuário). B: seguro. C: vulnerável (f-string). D: seguro. E: vulnerável (concatenação com valor da query string).

**5.** Não. O `PreparedStatement` só protege quando se usam `?` e `setXxx`. Aqui a entrada continua concatenada.

**6.**
```java
PreparedStatement ps = conn.prepareStatement("SELECT * FROM produtos WHERE id = ?");
ps.setInt(1, id);
```

**7.**
a) A entrada `termo` é concatenada ao SQL, permitindo alterar a consulta.
b)
```java
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM produtos WHERE nome LIKE ?");
ps.setString(1, "%" + termo + "%");
```

**8.** Não funcionam para nomes de coluna. Usar uma *allowlist*: aceitar só valores como `nome` e `preco`, e usar um padrão se vier outra coisa.

**9.** Exemplo:
- Título: SQL Injection na tela de login
- Local: `POST /login`, parâmetro `username`
- Categoria: OWASP Top 10, Injection
- Evidência: `admin'--` autenticou como administrador sem senha
- Impacto: acesso administrativo não autorizado; exposição de dados
- Severidade: crítica
- Correção: consultas parametrizadas

**10.**
```java
@Test void injecaoDeveFalhar() throws Exception {
    assertFalse(auth.login("admin'--", "qualquer"));
}
@Test void loginLegitimoDeveFuncionar() throws Exception {
    assertTrue(auth.login("maria", "abc123"));
}
```

**11.** Exemplos: usuário do banco com privilégio mínimo; mensagens de erro genéricas; logs e alertas de tentativas suspeitas; hash + salt para senhas; validação de entrada; revisão de código.
</details>

## 5.4 Modelo de relatório do laboratório

```markdown
# Relatório — Laboratório SQL Injection

**Aluno:**
**Data:**

## 1. Mapeamento
- Página testada:
- Requisição (método/URL):
- Parâmetros controlados pelo usuário:

## 2. Comportamento normal
| Teste | Entrada | Resultado |
|---|---|---|
| Login correto | | |
| Senha errada | | |
| Usuário inexistente | | |

## 3. Testes de segurança
| Teste | Entrada | Resultado observado |
|---|---|---|
| Aspa simples | | |
| Bypass de login | | |

## 4. Vulnerabilidade
- Título:
- Categoria OWASP:
- Causa raiz (trecho de código):
- Impacto:
- Severidade:

## 5. Correção aplicada
(cole o código antes/depois)

## 6. Reteste
| Teste | Antes | Depois |
|---|---|---|
| Login legítimo | | |
| Aspa simples | | |
| Bypass de login | | |

## 7. Conclusões
```

## 5.5 Autoavaliação

Ao terminar, você consegue:

- [ ] explicar o que é a OWASP e o Top 10?
- [ ] explicar com suas palavras por que SQL Injection acontece?
- [ ] reconhecer código vulnerável em Java, Python e JavaScript?
- [ ] corrigir uma consulta com parâmetros?
- [ ] descrever o impacto de forma que um gestor entenda?
- [ ] montar um reteste que prove a correção?
