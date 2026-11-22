# 1. Introdução ao OWASP Top 10

## 1.1 O que é a OWASP?

**OWASP** significa *Open Worldwide Application Security Project* (antes, *Open Web Application Security Project*).

É uma **fundação sem fins lucrativos**, mantida por uma comunidade global de voluntários, que produz material **gratuito e aberto** sobre segurança de software: guias, ferramentas, padrões e projetos.

Exemplos de projetos conhecidos:

| Projeto | Para que serve |
|---|---|
| **OWASP Top 10** | Lista dos riscos mais críticos em aplicações web |
| **OWASP ZAP** | Ferramenta gratuita de análise de segurança (veremos no módulo 4) |
| **OWASP Cheat Sheet Series** | Guias curtos de "como fazer certo" |
| **OWASP ASVS** | Padrão de requisitos de segurança para verificação |
| **OWASP Juice Shop / WebGoat** | Aplicações vulneráveis de propósito, para treino |

> **Ideia-chave:** a OWASP não vende nada. Seu material é uma referência neutra, usada por desenvolvedores, empresas e auditores no mundo todo.

## 1.2 O que é o OWASP Top 10?

É um **documento de conscientização** que lista as **10 categorias de risco** mais críticas para aplicações web, com base em dados reais de milhares de aplicações e em pesquisa da comunidade.

Pontos importantes:

- **É uma lista de categorias**, não de 10 vulnerabilidades específicas. Cada categoria agrupa várias falhas parecidas.
- **É atualizada periodicamente** (as edições mais conhecidas são 2017, 2021 e 2025). As posições mudam conforme o cenário muda.
- **Não é um checklist completo.** É o *mínimo* que todo mundo deveria conhecer, não tudo o que existe.

## 1.3 Por que conhecer as principais vulnerabilidades?

Uma analogia: **a casa e as fechaduras.**

```text
Você não precisa conhecer todos os tipos de ladrão do mundo.
Precisa saber que: portas destrancadas, janelas abertas e
chaves escondidas embaixo do tapete são os problemas mais comuns.
```

O Top 10 é essa lista de "portas destrancadas" da web. Conhecê-lo serve para:

1. **Falar a mesma língua.** Equipes, clientes e auditores usam os mesmos nomes.
2. **Priorizar.** Com tempo limitado, comece pelo que mais acontece e mais machuca.
3. **Prevenir.** A maioria dos incidentes vem de erros conhecidos e evitáveis.
4. **Atender exigências.** Muitos contratos e normas citam o OWASP Top 10.

## 1.4 As categorias (edição 2021)

A edição 2021 é a mais citada em materiais, cursos e relatórios.

| Código | Categoria | Em uma frase |
|---|---|---|
| **A01** | Broken Access Control (Controle de acesso quebrado) | O usuário acessa o que não deveria |
| **A02** | Cryptographic Failures (Falhas criptográficas) | Dados sensíveis mal protegidos |
| **A03** | **Injection (Injeção)** | Dados do usuário viram **comandos** (SQL, SO, etc.) |
| **A04** | Insecure Design (Design inseguro) | O projeto já nasce sem pensar em segurança |
| **A05** | Security Misconfiguration (Configuração incorreta) | Padrões inseguros, debug ligado, permissões demais |
| **A06** | Vulnerable and Outdated Components (Componentes vulneráveis) | Bibliotecas desatualizadas |
| **A07** | Identification and Authentication Failures | Login, senha e sessão mal tratados |
| **A08** | Software and Data Integrity Failures | Atualizações/dados sem verificar integridade |
| **A09** | Security Logging and Monitoring Failures | Ninguém percebe o ataque acontecendo |
| **A10** | Server-Side Request Forgery (SSRF) | O servidor é induzido a fazer requisições indevidas |

> **Nota sobre a edição 2025:** a OWASP publicou uma atualização da lista, que reorganizou categorias e as renumerou (por exemplo, *Injection* deixou de ser A03). Confira a versão vigente em <https://owasp.org/Top10/>. O conceito de injeção, foco deste módulo, permanece entre as categorias principais em todas as edições.

### Onde o SQL Injection se encaixa?

SQL Injection pertence à categoria **Injection**. Ela existe desde as primeiras edições do Top 10 e é um dos exemplos mais clássicos de como **misturar dados com comandos** gera uma falha grave.

```text
Injection
 ├── SQL Injection        ← foco deste módulo
 ├── Command Injection    (comandos do sistema operacional)
 ├── LDAP Injection
 ├── NoSQL Injection
 └── XSS (na edição 2021 foi incluído aqui; veremos no módulo 3)
```

## 1.5 Exemplos rápidos de cada categoria

Um exemplo do dia a dia para fixar cada uma:

| Categoria | Exemplo simples |
|---|---|
| Controle de acesso | Mudar `/pedido/100` para `/pedido/101` na URL e ver o pedido de outra pessoa |
| Falhas criptográficas | Senhas guardadas em texto puro no banco |
| **Injection** | Campo de login que aceita texto capaz de alterar a consulta SQL |
| Design inseguro | Recuperação de senha cuja "pergunta secreta" é o nome do pet |
| Configuração incorreta | Página de erro que mostra o *stack trace* completo |
| Componentes vulneráveis | Usar uma biblioteca com falha conhecida e nunca atualizar |
| Falhas de autenticação | Aceitar a senha `123456` e permitir tentativas ilimitadas |
| Integridade | Baixar um plugin sem verificar se foi adulterado |
| Logs e monitoramento | Mil tentativas de login falhas e nenhum alerta |
| SSRF | Campo "importar imagem por URL" que acessa serviços internos |

Neste curso aprofundamos: **Injection (SQL Injection)**, **XSS**, **Controle de Acesso**, além de autenticação e boas práticas.

## 1.6 Como usar o Top 10 como referência de análise

O Top 10 funciona como um **roteiro de perguntas** diante de uma aplicação:

```text
Para cada funcionalidade, pergunte:

 • Quem pode acessar isso?                → A01 Controle de acesso
 • Esse dado trafega/é guardado protegido? → A02 Criptografia
 • Essa entrada vira comando em algum lugar? → A03 Injection
 • Esse fluxo foi pensado com segurança?   → A04 Design
 • A configuração é segura?                → A05 Configuração
 • As bibliotecas estão atualizadas?       → A06 Componentes
 • O login está bem protegido?             → A07 Autenticação
 • Posso confiar na origem desse código?   → A08 Integridade
 • Eu saberia se estivessem atacando?      → A09 Logs
 • O servidor pode ser induzido a algo?    → A10 SSRF
```

### Passo a passo prático

1. **Mapeie** a aplicação (páginas, formulários, APIs, parâmetros).
2. **Associe** cada ponto às categorias do Top 10 que podem se aplicar.
3. **Teste** (só em ambiente autorizado) as hipóteses mais prováveis.
4. **Registre** o que encontrou, citando a categoria.
5. **Corrija e reteste.**

## 1.7 Cuidados ao usar o Top 10

- **Não é um padrão de conformidade completo.** Estar "livre do Top 10" não significa estar seguro.
- **Categorias, não receitas.** Cada uma exige estudo próprio.
- **A ordem reflete frequência e impacto, não a facilidade de exploração.**

## Fixando

1. O que é a OWASP? Ela vende alguma coisa?
2. O Top 10 é uma lista de vulnerabilidades ou de categorias de risco? Qual a diferença?
3. Em qual categoria o SQL Injection se encaixa?
4. Por que o Top 10 muda de uma edição para outra?

<details>
<summary>Respostas</summary>

1. Fundação sem fins lucrativos que produz material aberto sobre segurança. Não vende nada.
2. De categorias. Cada categoria agrupa várias falhas parecidas.
3. Injection.
4. Porque o cenário muda: novas tecnologias, novos ataques e novos dados coletados.
</details>
