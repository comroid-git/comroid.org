package org.comroid.auth.entity;

import org.comroid.api.BitmaskAttribute;
import org.comroid.util.Bitmask;

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
    @Column
    private Permit permit;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public UUID getId() {
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

    public Permit getPermit() {
        return permit;
    }

    public boolean hasPermission(Permit permission) {
        return Bitmask.isFlagSet(getPermit(), permission);
    }

    public enum Permit implements BitmaskAttribute<Permit> {
        None,
        Hub,
        Services,
        Admin
    }
}
