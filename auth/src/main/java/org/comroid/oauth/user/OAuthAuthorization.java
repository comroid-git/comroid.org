package org.comroid.oauth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.service.Service;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.mutatio.model.Ref;
import org.comroid.oauth.model.ValidityStage;
import org.comroid.oauth.rest.request.AuthenticationRequest;
import org.comroid.uniform.Context;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.java_websocket.util.Base64;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class OAuthAuthorization extends DataContainerBase<OAuthAuthorization> implements ValidityStage {
    @RootBind
    public static final GroupBind<OAuthAuthorization> Type
            = new GroupBind<>(AuthServer.MASTER_CONTEXT, "oauth-session");
    public static final VarBind<OAuthAuthorization, String, Service, Service> SERVICE
            = Type.createBind("service_id")
            .extractAs(StandardValueType.STRING)
            .andRemapRef(uuid -> AuthServer.instance.getServiceManager()
                    .getService(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, UserAccount, UserAccount> ACCOUNT
            = Type.createBind("account_id")
            .extractAs(StandardValueType.STRING)
            .andRemapRef(uuid -> AuthServer.instance.getUserManager()
                    .getUser(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorization, String, String[], Permit.Set> SCOPES
            = Type.createBind("scope")
            .extractAs(StandardValueType.STRING)
            .andRemap(str -> str.split(AuthenticationRequest.SCOPE_SPLIT_PATTERN))
            .reformatRefs(refs -> Permit.valueOf(refs
                    .streamValues()
                    .flatMap(Stream::of)
                    .toArray(String[]::new)))
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
    public final Ref<Service> service = getComputedReference(SERVICE);
    public final Ref<UserAccount> account = getComputedReference(ACCOUNT);
    public final Ref<Permit.Set> scopes = getComputedReference(SCOPES);
    public final Ref<String> userAgent = getComputedReference(USER_AGENT);
    public final Ref<String> code = getComputedReference(CODE);
    // fixme temp
    private final CompletableFuture<Void> invalidation = new CompletableFuture<>();

    public Service getService() {
        return service.assertion("service");
    }

    public UserAccount getAccount() {
        return account.assertion("user account");
    }

    public Permit.Set getScopes() {
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

    public OAuthAuthorization(Context context, UserAccount userAccount, Service service, String userAgent, Permit.Set scopes) {
        super(context, obj -> {
            obj.put(ACCOUNT, userAccount.getUUID().toString());
            obj.put(SERVICE, service.getUUID().toString());
            obj.put(USER_AGENT, userAgent);
            obj.put(CODE, generateCode(userAccount, service, userAgent));
            obj.put(SCOPES, scopes.toString());
        });
    }

    private static String generateCode(UserAccount userAccount, Service service, String userAgent) {
        String code = String.format("%s-%s-%s", userAccount.getUUID(), service.getUUID(), UUID.randomUUID());
        // fixme Remove log statement
        logger.trace("Generated auth code: {}", code);
        return code;
        //return Base64.encodeBytes(code.getBytes());
    }

    private static String generateToken(OAuthAuthorization authorization) {
        UserAccount userAccount = authorization.getAccount();
        Service service = authorization.getService();
        String code = String.format("%s-%s-%s", userAccount.getUUID(), service.getUUID(), UUID.randomUUID());
        code = Base64.encodeBytes(code.getBytes());
        // fixme Remove log statement
        logger.trace("Generated access token: {}", code);
        return code;
    }

    @Override
    public boolean invalidate() {
        return invalidation.complete(null);
    }

    public AccessToken createAccessToken() {
        AccessToken accessToken = new AccessToken(upgrade(Context.class), this, Duration.ofHours(12));
        if (!getAccount().addAccessToken(accessToken))
            throw new IllegalStateException("Could not add AccessToken to User");
        return accessToken;
    }

    public static final class AccessToken extends DataContainerBase<AccessToken> implements ValidityStage {
        @RootBind
        public static final GroupBind<AccessToken> Type = new GroupBind<>(AuthServer.MASTER_CONTEXT, "access-token");
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
        public static final VarBind<AccessToken, String, String[], Permit.Set> SCOPES
                = Type.createBind("scope")
                .extractAs(StandardValueType.STRING)
                .andRemap(str -> str.split(AuthenticationRequest.SCOPE_SPLIT_PATTERN))
                .reformatRefs(refs -> Permit.valueOf(refs
                        .streamValues()
                        .flatMap(Stream::of)
                        .toArray(String[]::new)))
                .setRequired()
                .build();
        public final Ref<String> token = getComputedReference(TOKEN);
        public final Ref<String> type = getComputedReference(TYPE);
        public final Ref<Duration> expiresAfter = getComputedReference(EXPIRES_IN);
        public final Ref<Permit.Set> scopes = getComputedReference(SCOPES);
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

        public Permit.Set getScopes() {
            return scopes.assertion("scopes");
        }

        public boolean isExpired() {
            Instant plus = createdAt.plus(getExpirationDuration());
            return plus.isBefore(Instant.now());
        }

        @Override
        public boolean isValid() {
            boolean expired = isExpired();
            boolean valid = authorization.isValid();
            boolean done = invalidation.isDone();
            boolean cancelled = invalidation.isCancelled();
            boolean b = !expired && valid && !done && !cancelled;
            logger.info("isValid? {}: expired = {}; authValid = {}; invDone = {}; invCancel = {}", b,expired,valid,done,cancelled);
            return b;
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
            boolean b = this.token.contentEquals(token);
            logger.info("equals? {}", b);
            return b;
        }
    }
}
