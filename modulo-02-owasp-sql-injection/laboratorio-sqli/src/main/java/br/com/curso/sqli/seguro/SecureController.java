package br.com.curso.sqli.seguro;

import br.com.curso.sqli.Dtos.LoginRequest;
import br.com.curso.sqli.Dtos.Product;
import br.com.curso.sqli.Dtos.UserInfo;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * ✅ API SEGURA: mesmos endpoints da versão vulnerável, com as defesas aplicadas:
 * consultas parametrizadas, allowlist, validação de entrada e mensagens de erro genéricas.
 */
@RestController
@RequestMapping("/seguro")
public class SecureController {

    private static final Logger log = LoggerFactory.getLogger(SecureController.class);
    private static final int TAMANHO_MAXIMO = 50;

    private final SecureRepository repository;

    public SecureController(SecureRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // Camada extra: validação de entrada (NÃO substitui a consulta parametrizada).
        if (invalido(request.username()) || invalido(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Requisição inválida"));
        }
        try {
            UserInfo user = repository.login(request.username(), request.password()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("mensagem", "Usuário ou senha inválidos"));
            }
            return ResponseEntity.ok(Map.of("mensagem", "Login realizado com sucesso", "usuario", user));
        } catch (DataAccessException e) {
            log.error("Erro ao autenticar", e); // detalhe fica no LOG do servidor...
            return erroGenerico();              // ...e o cliente recebe só uma mensagem genérica
        }
    }

    @GetMapping("/produtos")
    public ResponseEntity<Map<String, Object>> produtos(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) String ordenar) {
        if (busca.length() > TAMANHO_MAXIMO) {
            return ResponseEntity.badRequest().body(Map.of("mensagem", "Requisição inválida"));
        }
        try {
            List<Product> produtos = repository.search(busca, ordenar);
            return ResponseEntity.ok(Map.of("resultado", produtos));
        } catch (DataAccessException e) {
            log.error("Erro ao buscar produtos", e);
            return erroGenerico();
        }
    }

    private static boolean invalido(String valor) {
        return valor == null || valor.isBlank() || valor.length() > TAMANHO_MAXIMO;
    }

    private static ResponseEntity<Map<String, Object>> erroGenerico() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensagem", "Erro interno. Tente novamente mais tarde."));
    }
}
