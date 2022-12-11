package org.comroid.auth.entity;

import org.comroid.auth.repo.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import javax.annotation.PostConstruct;
import javax.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Entity
@Table(name = "services")
public class AuthService extends RegisteredClient implements AuthEntity {
    private static final ClientSettings clientSettings = ClientSettings.builder().build();
    private static final TokenSettings tokenSettings = TokenSettings.builder().build();
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

    @Override
    public String getClientId() {
        return getId();
    }

    @Override
    public Instant getClientIdIssuedAt() {
        return Instant.ofEpochMilli(getUUID().timestamp());
    }

    @Column
    @Override
    public String getClientSecret() {
        return secret;
    }

    @Override
    public Instant getClientSecretExpiresAt() {
        return Instant.ofEpochMilli(secretExpiry);
    }

    @Override
    public String getClientName() {
        return getName();
    }

    @Override
    public Set<ClientAuthenticationMethod> getClientAuthenticationMethods() {
        return Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
    }

    @Override
    public Set<AuthorizationGrantType> getAuthorizationGrantTypes() {
        return Set.of(AuthorizationGrantType.AUTHORIZATION_CODE);
    }

    @Override
    public Set<String> getRedirectUris() {
        return Set.of(getCallbackUrl());
    }

    @Override
    public Set<String> getScopes() {
        return Set.of(getRequiredScope());
    }

    @Override
    public ClientSettings getClientSettings() {
        return clientSettings;
    }

    @Override
    public TokenSettings getTokenSettings() {
        return tokenSettings;
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
