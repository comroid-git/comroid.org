package org.comroid.auth.entity;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "services")
public class AuthService extends RegisteredClient implements AuthEntity {
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
        return super.getClientSecret();
    }

    @Override
    public Instant getClientSecretExpiresAt() {
        return super.getClientSecretExpiresAt();
    }

    @Override
    public String getClientName() {
        return getName();
    }

    @Override
    public Set<ClientAuthenticationMethod> getClientAuthenticationMethods() {
        return super.getClientAuthenticationMethods();
    }

    @Override
    public Set<AuthorizationGrantType> getAuthorizationGrantTypes() {
        return super.getAuthorizationGrantTypes();
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
        return super.getClientSettings();
    }

    @Override
    public TokenSettings getTokenSettings() {
        return super.getTokenSettings();
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
    }
}
