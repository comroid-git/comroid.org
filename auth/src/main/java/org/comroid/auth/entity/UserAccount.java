package org.comroid.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements AuthEntity {
    @Id
    private UUID uuid;
    @Column
    private String username;
    @Column
    private String email;
    @Column
    private String passwordHash;
    @Column
    private boolean emailVerified;
    @Column
    private String sessionId;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getSessionId() {
        return sessionId;
    }
}
