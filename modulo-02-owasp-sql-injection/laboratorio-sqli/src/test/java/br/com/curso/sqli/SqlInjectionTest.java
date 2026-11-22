package br.com.curso.sqli;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Prova automatizada: o mesmo ataque funciona na API vulnerável e falha na API segura. */
@SpringBootTest
@AutoConfigureMockMvc
class SqlInjectionTest {

    @Autowired
    MockMvc mvc;

    private ResultActions login(String base, String user, String pass) throws Exception {
        String json = "{\"username\":%s,\"password\":%s}".formatted(quote(user), quote(pass));
        return mvc.perform(post(base + "/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Nested
    @DisplayName("API vulnerável (/vulneravel)")
    class Vulneravel {

        @Test
        @DisplayName("login normal funciona")
        void loginNormal() throws Exception {
            login("/vulneravel", "maria", "abc123").andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.name").value("maria"));
        }

        @Test
        @DisplayName("senha errada é recusada")
        void senhaErrada() throws Exception {
            login("/vulneravel", "maria", "errada").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("aspa simples quebra a consulta (sinal de SQLi)")
        void aspaSimples() throws Exception {
            login("/vulneravel", "'", "x").andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.erroDoBanco").exists());
        }

        @Test
        @DisplayName("admin'-- autentica como ADMIN sem senha")
        void bypassComComentario() throws Exception {
            login("/vulneravel", "admin'--", "qualquer").andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.role").value("ADMIN"));
        }

        @Test
        @DisplayName("' OR '1'='1 na senha autentica sem conhecer credenciais")
        void condicaoSempreVerdadeira() throws Exception {
            login("/vulneravel", "x", "' OR '1'='1").andExpect(status().isOk());
        }

        @Test
        @DisplayName("UNION vaza usuários e senhas pela busca de produtos")
        void unionVazaDados() throws Exception {
            mvc.perform(get("/vulneravel/produtos")
                            .param("busca", "zzz' UNION SELECT id, name, password FROM users --"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado[?(@.description == 'Adm!n#2024')]").exists());
        }

        @Test
        @DisplayName("ORDER BY injetável: subconsulta condicional (blind)")
        void orderByInjetavel() throws Exception {
            mvc.perform(get("/vulneravel/produtos")
                            .param("ordenar", "CASE WHEN (SELECT COUNT(*) FROM users) > 0 THEN -id ELSE id END"))
                    .andExpect(status().isOk())
                    // a condição sobre OUTRA tabela inverteu a ordem: o atacante "pergunta" ao banco
                    .andExpect(jsonPath("$.resultado[0].id").value(4));
        }
    }

    @Nested
    @DisplayName("API segura (/seguro)")
    class Seguro {

        @Test
        @DisplayName("login normal continua funcionando")
        void loginNormal() throws Exception {
            login("/seguro", "maria", "abc123").andExpect(status().isOk())
                    .andExpect(jsonPath("$.usuario.name").value("maria"));
        }

        @Test
        @DisplayName("aspa simples é tratada como texto comum: 401, sem erro 500")
        void aspaSimples() throws Exception {
            login("/seguro", "'", "x").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("admin'-- NÃO autentica")
        void bypassComComentario() throws Exception {
            login("/seguro", "admin'--", "qualquer").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("' OR '1'='1 NÃO autentica")
        void condicaoSempreVerdadeira() throws Exception {
            login("/seguro", "x", "' OR '1'='1").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("UNION vira apenas um texto de busca: nenhum resultado, nenhum vazamento")
        void unionNaoVaza() throws Exception {
            mvc.perform(get("/seguro/produtos")
                            .param("busca", "zzz' UNION SELECT id, name, password FROM users --"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado").isEmpty());
        }

        @Test
        @DisplayName("ORDER BY malicioso é ignorado pela allowlist")
        void orderByIgnorado() throws Exception {
            mvc.perform(get("/seguro/produtos")
                            .param("ordenar", "CASE WHEN (SELECT COUNT(*) FROM users) > 0 THEN -id ELSE id END"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado[0].id").value(1));
        }

        @Test
        @DisplayName("ordenação válida pela allowlist funciona")
        void ordenacaoValida() throws Exception {
            mvc.perform(get("/seguro/produtos").param("ordenar", "nome"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado[0].name").value("Monitor"));
        }

        @Test
        @DisplayName("busca legítima retorna o produto esperado")
        void buscaNormal() throws Exception {
            mvc.perform(get("/seguro/produtos").param("busca", "Mouse"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado[0].name").value("Mouse"));
        }
    }
}
