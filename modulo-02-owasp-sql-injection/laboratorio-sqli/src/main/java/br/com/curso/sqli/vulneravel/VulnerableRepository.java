package br.com.curso.sqli.vulneravel;

import br.com.curso.sqli.Dtos.Product;
import br.com.curso.sqli.Dtos.UserInfo;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * ❌ VULNERÁVEL — NÃO COPIE ESTE CÓDIGO PARA PRODUÇÃO.
 *
 * <p>Todas as consultas são montadas por CONCATENAÇÃO de strings com dados vindos do usuário.
 * A entrada deixa de ser "dado" e passa a fazer parte da estrutura do comando SQL.
 */
@Repository
public class VulnerableRepository {

    private final JdbcClient jdbc;

    public VulnerableRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Devolve o SQL montado, para a aula mostrar exatamente o que o banco recebe. */
    public String buildLoginSql(String username, String password) {
        return "SELECT id, name, role FROM users"
                + " WHERE name = '" + username + "'"          // ❌ concatenação
                + " AND password = '" + password + "'";       // ❌ concatenação
    }

    public Optional<UserInfo> login(String sql) {
        return jdbc.sql(sql)
                .query((rs, n) -> new UserInfo(rs.getInt("id"), rs.getString("name"), rs.getString("role")))
                .list()
                .stream()
                .findFirst(); // regra da aplicação: "veio linha => login válido"
    }

    public String buildSearchSql(String busca, String ordenar) {
        String sql = "SELECT id, name, description FROM products"
                + " WHERE name LIKE '%" + busca + "%'";       // ❌ concatenação
        if (ordenar != null && !ordenar.isBlank()) {
            sql += " ORDER BY " + ordenar;                    // ❌ concatenação (nem aceitaria parâmetro)
        }
        return sql;
    }

    public List<Product> search(String sql) {
        return jdbc.sql(sql)
                .query((rs, n) -> new Product(rs.getInt(1), rs.getString(2), rs.getString(3)))
                .list();
    }
}
