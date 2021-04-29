package org.comroid.oauth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.mutatio.model.Ref;
import org.comroid.oauth.OAuth;
import org.comroid.oauth.client.Client;
import org.comroid.oauth.client.ClientProvider;
import org.comroid.oauth.model.ValidityStage;
import org.comroid.oauth.resource.Resource;
import org.comroid.oauth.resource.ResourceProvider;
import org.comroid.oauth.rest.request.AuthenticationRequest;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.java_websocket.util.Base64;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OAuthAuthorization extends DataContainerBase<OAuthAuthorization> implements ValidityStage {
    @RootBind
    public static final GroupBind<OAuthAuthorization> Type
            = new GroupBind<>(OAuth.CONTEXT, "oauth-session");
    public static final VarBind<OAuthAuthorization, String, ? extends Resource, ? extends Resource> SERVICE
            = Type.createBind("service_id")
            .extractAs(StandardValueType.STRING)
            .andResolveRef((it, uuid) -> it.requireFromContext(ResourceProvider.class)
                    .getResource(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, ? extends Client, ? extends Client> ACCOUNT
            = Type.createBind("account_id")
            .extractAs(StandardValueType.STRING)
            .andResolveRef((it, uuid) -> it.requireFromContext(ClientProvider.class)
                    .findClient(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, String[], Set<String>> SCOPES
            = Type.createBind("scope")
            .extractAs(StandardValueType.STRING)
            .andRemap(str -> str.split(AuthenticationRequest.SCOPE_SPLIT_PATTERN))
            .reformatRefs(refs -> Collections.unmodifiableSet(refs
                    .streamValues()
                    .flatMap(Stream::of)
                    .collect(Collectors.toSet())))
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, String, String> USER_AGENT
            = Type.createBind("user_agent")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, String, String> CODE
            = Type.createBind("code")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final String BEARER_PREFIX = "Bearer ";
    private static final Logger logger = LogManager.getLogger();
    public final Ref<? extends Resource> service = getComputedReference(SERVICE);
    public final Ref<? extends Client> account = getComputedReference(ACCOUNT);
    public final Ref<Set<String>> scopes = getComputedReference(SCOPES);
    public final Ref<String> userAgent = getComputedReference(USER_AGENT);
    public final Ref<String> code = getComputedReference(CODE);
    // fixme temp
    private final CompletableFuture<Void> invalidation = new CompletableFuture<>();

    public Resource getResource() {
        return service.assertion("service");
    }

    public Client getClient() {
        return account.assertion("user account");
    }

    public Set<String> getScopes() {
        return scopes.assertion("scopes");
    }

    public String getUserAgent() {
        return userAgent.assertion("user agent");
    }

    public String getCode() {
        return code.assertion("code");
    }

    @Override
    public boolean isValid() {
        return !invalidation.isDone() && !invalidation.isCancelled();
    }

    public UniNode getClientData() {
        return getClient().getUserInfo();
    }

    public OAuthAuthorization(Context context, Client client, Resource resource, String userAgent, String... scopes) {
        super(context, obj -> {
            obj.put(ACCOUNT, client.getUUID().toString());
            obj.put(SERVICE, resource.getUUID().toString());
            obj.put(USER_AGENT, userAgent);
            obj.put(CODE, client.generateAuthorizationToken(resource, userAgent));
            obj.put(SCOPES, String.join(" ", scopes));
        });
    }

    private static String generateToken(OAuthAuthorization authorization) {
        return authorization.getResource().generateAccessToken(authorization);
    }

    @Override
    public boolean invalidate() {
        return invalidation.complete(null);
    }

    public AccessToken createAccessToken() {
        AccessToken accessToken = new AccessToken(upgrade(Context.class), this, Duration.ofHours(12));
        if (!getClient().addAccessToken(accessToken))
            throw new IllegalStateException("Could not add AccessToken to User");
        return accessToken;
    }

    public static final class AccessToken extends DataContainerBase<AccessToken> implements ValidityStage {
        @RootBind
        public static final GroupBind<AccessToken> Type = new GroupBind<>(OAuth.CONTEXT, "access-token");
        public static final VarBind<AccessToken, String, String, String> TOKEN
                = Type.createBind("access_token")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired()
                .build();
        public static final VarBind<AccessToken, String, String, String> TYPE
                = Type.createBind("token_type")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired()
                .build();
        public static final VarBind<AccessToken, Long, Duration, Duration> EXPIRES_IN
                = Type.createBind("expires_in")
                .extractAs(StandardValueType.LONG)
                .andRemap(Duration::ofSeconds)
                .onceEach()
                .setRequired()
                .build();
        public static final VarBind<AccessToken, String, String[], Set<String>> SCOPES
                = Type.createBind("scope")
                .extractAs(StandardValueType.STRING)
                .andRemap(str -> str.split(AuthenticationRequest.SCOPE_SPLIT_PATTERN))
                .reformatRefs(refs -> Collections.unmodifiableSet(refs
                        .streamValues()
                        .flatMap(Stream::of)
                        .collect(Collectors.toSet())))
                .setRequired()
                .build();
        public final Ref<String> token = getComputedReference(TOKEN);
        public final Ref<String> type = getComputedReference(TYPE);
        public final Ref<Duration> expiresAfter = getComputedReference(EXPIRES_IN);
        public final Ref<Set<String>> scopes = getComputedReference(SCOPES);
        private final OAuthAuthorization authorization;
        // fixme temp
        private final CompletableFuture<Void> invalidation = new CompletableFuture<>();
        private final Instant createdAt;

        public OAuthAuthorization getAuthorization() {
            return authorization;
        }

        public String getToken() {
            return token.assertion("token");
        }

        public String getType() {
            return type.assertion("type");
        }

        public Duration getExpirationDuration() {
            return expiresAfter.assertion("expiry");
        }

        public Set<String> getScopes() {
            return scopes.assertion("scopes");
        }

        public boolean isExpired() {
            Instant plus = createdAt.plus(getExpirationDuration());
            return plus.isBefore(Instant.now());
        }

        @Override
        public boolean isValid() {
            return !isExpired() && authorization.isValid() && !invalidation.isDone() && !invalidation.isCancelled();
        }

        private AccessToken(Context context, final OAuthAuthorization authorization, @Nullable Duration expiration) {
            super(context, obj -> {
                obj.put(TOKEN, generateToken(authorization));
                obj.put(TYPE, "bearer");
                obj.put(EXPIRES_IN, expiration.getSeconds());
                obj.put(SCOPES, authorization.getScopes().toString());
            });
            this.authorization = authorization;
            this.createdAt = Instant.now();
        }

        @Override
        public boolean invalidate() {
            return invalidation.complete(null);
        }

        public boolean checkToken(String token) {
            return this.token.contentEquals(token);
        }
    }
}
