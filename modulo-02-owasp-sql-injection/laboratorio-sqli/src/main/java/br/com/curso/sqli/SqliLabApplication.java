package br.com.curso.sqli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Laboratório de SQL Injection.
 *
 * <p>ATENÇÃO: esta aplicação contém uma API <b>propositalmente vulnerável</b> (/vulneravel/**).
 * Execute-a apenas localmente, para estudo. Nunca publique em rede/Internet.
 */
@SpringBootApplication
public class SqliLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqliLabApplication.class, args);
    }
}
