package org.comroid.auth.entity;

import jakarta.persistence.*;
import org.comroid.auth.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Entity
@Table(name = "services")
public class AuthService implements AuthEntity {
    @Id
    private String uuid;
    @Column
    private String name;
    @Column
    private String url;
    @Column
    private String callbackUrl;
    @Column
    private String requiredScope;
    @Column
    private String secret;
    @Column
    private long secretExpiry;

    @Override
    public UUID getUUID() {
        return UUID.fromString(uuid);
    }

    public String getId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getRequiredScope() {
        return requiredScope;
    }

    public void setRequiredScope(String requiredScope) {
        this.requiredScope = requiredScope;
    }

    public AuthService() {
    }

    public AuthService(String name, String url, String callbackUrl, String requiredScope) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.url = url;
        this.callbackUrl = callbackUrl;
        this.requiredScope = requiredScope;
        regenerateSecret();
    }

    public void regenerateSecret() {
        secret = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        secretExpiry = Long.MAX_VALUE;
    }
}
