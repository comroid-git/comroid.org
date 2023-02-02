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
public class AuthService implements AuthEntity {
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
    @Transient
    private RegisteredClient client;

    public RegisteredClient getClient() {
        return client;
    }

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
        init();
    }

    @PostLoad
    public void init() {
        client = RegisteredClient.withId(getId())
                .clientId(getId())
                .clientName(name)
                .redirectUri(callbackUrl)
                .clientSecret(secret)
                .clientSecretExpiresAt(Instant.ofEpochMilli(secretExpiry))
                .scope(requiredScope)
                .clientAuthenticationMethods(mtd -> mtd.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC))
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .build();
    }

    public void regenerateSecret() {
        secret = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        secretExpiry = Long.MAX_VALUE;
        init();
    }
}
