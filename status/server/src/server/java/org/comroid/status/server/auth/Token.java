package org.comroid.status.server.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tokens")
public final class Token {
    @Id
    private String name;
    @Column
    private String token;

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public Token() {
        this(null, null);
    }

    public Token(String name, String token) {
        this.name = name;
        this.token = token;
    }
}
