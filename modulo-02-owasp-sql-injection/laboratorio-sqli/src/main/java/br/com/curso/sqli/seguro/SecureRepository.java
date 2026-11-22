package br.com.curso.sqli.seguro;

import br.com.curso.sqli.Dtos.Product;
import br.com.curso.sqli.Dtos.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * ✅ SEGURO — consultas parametrizadas.
 *
 * <p>O SQL é um texto FIXO, com marcadores (?). Os valores do usuário trafegam separados e o
 * driver JDBC os trata sempre como DADO, nunca como parte do comando.
 */
@Repository
public class SecureRepository {

    /**
     * Allowlist: nome de coluna NÃO pode ser parâmetro (?), então o valor recebido só serve de
     * "chave" para escolher um trecho de SQL que NÓS escrevemos.
     */
    private static final Map<String, String> ORDENACOES = Map.of(
            "id", "id",
            "nome", "name",
            "descricao", "description");

    private final JdbcClient jdbc;

    public SecureRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserInfo> login(String username, String password) {
        return jdbc.sql("SELECT id, name, role FROM users WHERE name = ? AND password = ?")
                .param(username)   // ✅ parâmetro 1
                .param(password)   // ✅ parâmetro 2
                .query((rs, n) -> new UserInfo(rs.getInt("id"), rs.getString("name"), rs.getString("role")))
                .optional();
    }

    public List<Product> search(String busca, String ordenar) {
        String coluna = ORDENACOES.getOrDefault(ordenar == null ? "" : ordenar, "id"); // ✅ allowlist
        return jdbc.sql("SELECT id, name, description FROM products WHERE name LIKE ? ORDER BY " + coluna)
                .param("%" + busca + "%") // ✅ o curinga % faz parte do VALOR, não do SQL
                .query((rs, n) -> new Product(rs.getInt(1), rs.getString(2), rs.getString(3)))
                .list();
    }
}
