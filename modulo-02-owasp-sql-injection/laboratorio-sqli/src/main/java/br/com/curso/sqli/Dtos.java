package br.com.curso.sqli;

/** Objetos de entrada/saída compartilhados pelas duas versões da API. */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(String username, String password) {
    }

    public record UserInfo(int id, String name, String role) {
    }

    public record Product(int id, String name, String description) {
    }
}
