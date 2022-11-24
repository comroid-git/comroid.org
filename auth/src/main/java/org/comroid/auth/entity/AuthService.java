package org.comroid.auth.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "services")
public class AuthService implements AuthEntity {
    @Id
    private UUID uuid;
    @Column
    private String name;
    @Column
    private String url;
    @Column
    private String callbackUrl;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }
}
