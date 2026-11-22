package br.com.curso.sqli.vulneravel;

import br.com.curso.sqli.Dtos.LoginRequest;
import br.com.curso.sqli.Dtos.Product;
import br.com.curso.sqli.Dtos.UserInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ❌ API VULNERÁVEL (didática).
 *
 * <p>Além de concatenar SQL, ela comete um segundo erro: devolve ao cliente o SQL executado e a
 * mensagem de erro do banco (configuração incorreta / vazamento de informação). Na vida real isso
 * ajudaria o atacante; aqui serve para o aluno enxergar o que acontece por trás.
 */
@RestController
@RequestMapping("/vulneravel")
public class VulnerableController {

    private final VulnerableRepository repository;

    public VulnerableController(VulnerableRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String sql = repository.buildLoginSql(request.username(), request.password());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sqlExecutado", sql);
        try {
            UserInfo user = repository.login(sql).orElse(null);
            if (user == null) {
                body.put("mensagem", "Usuário ou senha inválidos");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("mensagem", "Login realizado com sucesso");
            body.put("usuario", user);
            return ResponseEntity.ok(body);
        } catch (DataAccessException e) {
            body.put("mensagem", "Erro de banco de dados");
            body.put("erroDoBanco", e.getMostSpecificCause().getMessage()); // ❌ vaza detalhes internos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/produtos")
    public ResponseEntity<Map<String, Object>> produtos(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) String ordenar) {
        String sql = repository.buildSearchSql(busca, ordenar);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sqlExecutado", sql);
        try {
            List<Product> produtos = repository.search(sql);
            body.put("resultado", produtos);
            return ResponseEntity.ok(body);
        } catch (DataAccessException e) {
            body.put("mensagem", "Erro de banco de dados");
            body.put("erroDoBanco", e.getMostSpecificCause().getMessage()); // ❌ vaza detalhes internos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
