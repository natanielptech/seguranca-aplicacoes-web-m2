# 6. Resumo, glossário e referências

## 6.1 Resumo do módulo

### OWASP Top 10

- A **OWASP** é uma fundação aberta e sem fins lucrativos sobre segurança de software.
- O **Top 10** lista as **categorias** de risco mais críticas em aplicações web.
- Serve para **falar a mesma língua**, **priorizar** e **guiar a análise**.
- Não é um checklist completo e é atualizado periodicamente.

### SQL Injection

- Acontece quando **dados do usuário alteram a estrutura de uma consulta SQL**.
- **Causa raiz:** concatenar entrada do usuário na string SQL, misturando **dados** e **comandos**.
- **Impactos:** vazamento, alteração e exclusão de dados, bypass de autenticação, indisponibilidade.
- **Defesa principal:** **consultas parametrizadas**.
- **Defesa em camadas:** validação, privilégio mínimo, erros genéricos, logs, hash de senhas.
- **Fechar o ciclo:** reteste, de preferência automatizado.

## 6.2 Cola rápida

```text
❌  "SELECT ... WHERE name = '" + name + "'"      concatenação
✅  "SELECT ... WHERE name = ?"  +  setString()   parâmetro
```

| Pergunta de revisão de código | Resposta esperada |
|---|---|
| Há entrada do usuário dentro de uma string SQL? | Não |
| As consultas usam `?` / parâmetros nomeados? | Sim |
| Nomes de coluna/ordenação vêm de allowlist? | Sim |
| O usuário do banco tem privilégio mínimo? | Sim |
| Os erros ao usuário são genéricos? | Sim |

## 6.3 Checklist de prevenção

- [ ] Todas as consultas usam parâmetros
- [ ] Nenhuma consulta é montada por concatenação ou `format`
- [ ] ORM usado sem SQL "cru" com concatenação
- [ ] Ordenação e nomes de colunas via allowlist
- [ ] Validação de entrada no servidor
- [ ] Usuário do banco sem privilégios administrativos
- [ ] Mensagens de erro genéricas ao usuário
- [ ] Erros detalhados apenas nos logs do servidor
- [ ] Senhas armazenadas com hash + salt
- [ ] Testes automatizados cobrindo tentativas de injeção
- [ ] Logs e alertas para comportamento suspeito

## 6.4 Glossário

| Termo | Significado |
|---|---|
| **OWASP** | Fundação aberta dedicada à segurança de aplicações |
| **Top 10** | Lista das 10 categorias de risco mais críticas em aplicações web |
| **SQL** | Linguagem usada para consultar e alterar bancos relacionais |
| **SQL Injection** | Falha em que dados do usuário alteram a consulta SQL |
| **Injeção** | Classe de falhas em que dados não confiáveis são interpretados como comandos |
| **Concatenação** | Juntar textos em uma única string |
| **Consulta parametrizada** | Consulta com marcadores (`?`) e valores enviados separadamente |
| **PreparedStatement** | Recurso do Java (JDBC) para consultas parametrizadas |
| **Payload** | Entrada preparada para testar ou explorar uma falha |
| **Bypass de autenticação** | Entrar no sistema sem credenciais válidas |
| **Allowlist** | Lista de valores permitidos; o que não está nela é rejeitado |
| **Privilégio mínimo** | Conceder só as permissões estritamente necessárias |
| **Defesa em camadas** | Várias proteções combinadas, para que a falha de uma não seja fatal |
| **Reteste** | Repetir os testes após a correção para confirmar que funcionou |
| **ORM** | Ferramenta que mapeia objetos para tabelas (Hibernate, JPA, SQLAlchemy...) |
| **Stack trace** | Registro detalhado de um erro, com arquivos e linhas do código |
| **LGPD** | Lei Geral de Proteção de Dados (Brasil) |
| **Superfície de ataque** | Conjunto de pontos por onde um sistema pode ser atacado |

## 6.5 Perguntas frequentes

**"Se eu validar a entrada, ainda preciso parametrizar?"**
Sim. A validação ajuda, mas é frágil: textos livres e senhas precisam aceitar símbolos. A parametrização resolve a causa raiz.

**"Usar ORM me protege?"**
Em geral sim, porque ORMs parametrizam por padrão. Mas qualquer consulta "nativa" com concatenação volta a ser vulnerável.

**"Escapar aspas não é suficiente?"**
Escapar manualmente é propenso a erros e já foi contornado muitas vezes. Parametrize.

**"SQL Injection só afeta bancos relacionais?"**
Não. Bancos NoSQL e outras tecnologias também sofrem de injeção. O princípio (misturar dados e comandos) é o mesmo.

**"Quanto mais difícil de acertar a senha, mais seguro contra SQLi?"**
Não. SQLi contorna a verificação de senha; por isso a senha forte não ajuda contra esta falha.

## 6.6 Referências para aprofundar

- OWASP Top 10: <https://owasp.org/Top10/>
- OWASP SQL Injection Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html>
- OWASP Query Parameterization Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Query_Parameterization_Cheat_Sheet.html>
- OWASP Web Security Testing Guide (WSTG): <https://owasp.org/www-project-web-security-testing-guide/>
- PortSwigger Web Security Academy (laboratórios gratuitos): <https://portswigger.net/web-security/sql-injection>

## 6.7 Próximo módulo

**Módulo 3 — XSS e Falhas de Controle de Acesso.**

Você viu que o SQL Injection ocorre quando o **banco de dados** interpreta dados como comandos. No XSS, o mesmo raciocínio se repete, só que o intérprete é o **navegador**.

```text
SQL Injection:  dado do usuário  →  interpretado pelo BANCO
XSS:            dado do usuário  →  interpretado pelo NAVEGADOR
```
