# Módulo 2 — OWASP Top 10 e SQL Injection

**Carga horária:** 4 horas
**Pré-requisito:** Módulo 1 (HTTP, requisições, autenticação, banco de dados)

> **Aviso:** todas as técnicas deste módulo devem ser praticadas **somente** no laboratório do curso ou em ambientes próprios. Testar sistemas de terceiros sem autorização é crime.

---

## O que você vai aprender

Ao final deste módulo você será capaz de:

- explicar o que é a OWASP e o que é o OWASP Top 10;
- usar o Top 10 como mapa para pensar em segurança;
- explicar **por que** o SQL Injection acontece (e não apenas decorar um truque);
- reconhecer código vulnerável a SQL Injection;
- demonstrar o problema em um ambiente controlado e descrever seu impacto;
- corrigir o código com consultas parametrizadas;
- provar com um reteste que a correção funcionou.

## Como estudar

Cada tópico segue o ciclo do curso:

```text
CONCEITO → EXEMPLO → DEMONSTRAÇÃO → ANÁLISE DO CÓDIGO → CORREÇÃO → RETESTE → LABORATÓRIO
```

## Roteiro (4 horas)

| # | Arquivo | Conteúdo | Tempo |
|---|---|---|---:|
| 1 | [01-owasp-top-10.md](01-owasp-top-10.md) | O que é OWASP, o Top 10 e como usá-lo | 50 min |
| 2 | [02-sql-injection-conceitos.md](02-sql-injection-conceitos.md) | O que é SQL Injection, por que acontece, impactos | 60 min |
| 3 | [03-demonstracao-pratica.md](03-demonstracao-pratica.md) | Login vulnerável: comportamento normal x ataque | 50 min |
| 4 | [04-analise-codigo-e-correcao.md](04-analise-codigo-e-correcao.md) | Código inseguro, PreparedStatement, defesa em camadas, reteste | 50 min |
| 5 | [05-laboratorio-e-exercicios.md](05-laboratorio-e-exercicios.md) | Laboratório guiado e exercícios | 40 min |
| 6 | [06-resumo-e-glossario.md](06-resumo-e-glossario.md) | Resumo, checklist, glossário, referências | 10 min |
| 7 | [07-laboratorio-java-spring.md](07-laboratorio-java-spring.md) | Projeto Java/Spring Boot: API vulnerável x segura ([código](laboratorio-sqli)) | extra |

## Mapa do módulo

```text
        OWASP Top 10
   (visão geral dos riscos)
             │
             ▼
      Categoria Injection
             │
             ▼
       SQL Injection
             │
   ┌─────────┼─────────┐
   ▼         ▼         ▼
 Conceito  Demo     Correção
           (login)  (PreparedStatement)
             │
             ▼
          Reteste
```
