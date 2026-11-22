# laboratorio-sqli

Laboratório do Módulo 2: API Spring Boot com uma versão **vulnerável** (`/vulneravel/**`) e uma **segura** (`/seguro/**`) a SQL Injection.

- Java 25 (LTS) · Spring Boot 4.2 · Spring JDBC · H2 em memória
- ⚠️ Contém código propositalmente inseguro. Rode apenas localmente.

```bash
mvn spring-boot:run   # http://localhost:8080
mvn test              # ataques contra as duas versões
```

Interface visual para enviar os ataques: **http://localhost:8080** (com a aplicação rodando).

Console do banco H2: **http://localhost:8080/h2-console** (JDBC URL `jdbc:h2:mem:laboratorio`, user `sa`, senha vazia).

Guia completo (como o ataque funciona e como se proteger): [`../07-laboratorio-java-spring.md`](../07-laboratorio-java-spring.md).
Requisições prontas: [`requests.http`](requests.http).
