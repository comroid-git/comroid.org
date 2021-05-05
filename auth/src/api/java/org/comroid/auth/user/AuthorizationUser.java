package org.comroid.auth.user;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.EMailAddress;
import org.comroid.api.Polyfill;
import org.comroid.auth.model.AuthEntity;
import org.comroid.auth.model.User;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.webkit.model.CookieProvider;
import org.comroid.webkit.oauth.model.ValidityStage;
import org.comroid.webkit.oauth.rest.OAuthEndpoint;
import org.comroid.webkit.oauth.rest.request.TokenRevocationRequest;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class AuthorizationUser extends DataContainerBase<AuthEntity> implements ValidityStage, CookieProvider, User {
    @RootBind
    public static final GroupBind<AuthorizationUser> Type = User.Type.subGroup("authorization-user");
    public static final VarBind<AuthorizationUser, String, String, String> AUTHORIZATION_CODE
            = Type.createBind("authorization-code")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<AuthorizationUser, UniObjectNode, OAuthAuthorization.AccessToken, OAuthAuthorization.AccessToken> CURRENT_ACCESS_TOKEN
            = Type.createBind("current-access-token")
            .extractAsObject()
            .andResolve(OAuthAuthorization.AccessToken::new)
            .build();
    public static final VarBind<AuthorizationUser, String, String, String> COOKIE
            = Type.createBind("cookie")
            .extractAs(StandardValueType.STRING)
            .build();
    public static final VarBind<AuthorizationUser, Long, Instant, Instant> EXPIRY
            = Type.createBind("expiry")
            .extractAs(StandardValueType.LONG)
            .andRemap(Instant::ofEpochMilli)
            .build();
    private static final Logger logger = LogManager.getLogger();
    public final Ref<String> cookie = getComputedReference(COOKIE);
    public final Ref<Instant> expiry = getComputedReference(EXPIRY);
    public final Ref<UUID> uuid = getComputedReference(ID);
    public final Ref<String> username = getComputedReference(USERNAME);
    public final Ref<EMailAddress> email = getComputedReference(EMAIL);
    public final Ref<String> authCode = getComputedReference(AUTHORIZATION_CODE);
    public final Ref<OAuthAuthorization.AccessToken> accessToken = getComputedReference(CURRENT_ACCESS_TOKEN);
    protected final CompletableFuture<Void> invalidation = new CompletableFuture<>();
    private final CompletableFuture<? extends AuthorizationUser> initialValidation;

    public CompletableFuture<? extends AuthorizationUser> getInitialValidation() {
        return initialValidation;
    }

    public final String getAuthCode() {
        return authCode.assertion("auth code");
    }

    public final String getToken() {
        return accessToken.map(OAuthAuthorization.AccessToken::getToken).into(str -> "Bearer " + str);
    }

    public final boolean isValid() {
        return !invalidation.isDone() && getToken() != null && expiry.test(it -> it.isAfter(Instant.now()));
    }

    @Override
    public final UUID getUUID() {
        return uuid.requireNonNull(unavailableMessage("UUID"));
    }

    @Override
    public final String getUsername() {
        return username.requireNonNull(unavailableMessage("Username"));
    }

    @Override
    public final EMailAddress getEMailAddress() {
        return email.requireNonNull(unavailableMessage("EMail"));
    }

    @Override
    public final String getName() {
        return username.isNull() ? "Unknown Username" : getUsername();
    }

    @Override
    public final String getAlternateName() {
        return getUUID().toString();
    }

    @Override
    public String getPlainCookie() {
        return cookie.assertion("cookie");
    }

    public AuthorizationUser(ContextualProvider context, @Nullable Consumer<UniObjectNode> initialDataBuilder) {
        super(context, initialDataBuilder);

        this.initialValidation = validateUserInfo().exceptionally(Polyfill
                .exceptionLogger(logger, Level.FATAL, "Could not validate " + this));
    }

    private MessageSupplier unavailableMessage(String fieldName) {
        return MessageSupplier.format("Field '%s' unavailable; UserData was never validated", fieldName);
    }

    @Override
    public final boolean invalidate() {
        return invalidateAuthorizationCode().join() == this;
    }

    public final CompletableFuture<? extends AuthorizationUser> invalidateAuthorizationCode() {
        if (!isValid())
            return CompletableFuture.completedFuture(this);
        return getFromContext(REST.class)
                .orElseGet(() -> new REST(this))
                .request()
                .method(REST.Method.POST)
                .endpoint(OAuthEndpoint.TOKEN_REVOKE)
                .buildBody(BodyBuilderType.OBJECT, obj -> {
                    obj.put(TokenRevocationRequest.TOKEN, getAuthCode());
                    obj.put(TokenRevocationRequest.TOKEN_HINT, "authorization_code");
                })
                .execute()
                .thenApply(response -> {
                    if (response.getStatusCode() != 200)
                        throw response.toException();
                    invalidation.complete(null);
                    return this;
                });
    }

    public final CompletableFuture<? extends AuthorizationUser> invalidateToken() {
        if (!isValid())
            return CompletableFuture.completedFuture(this);
        return getFromContext(REST.class)
                .orElseGet(() -> new REST(this))
                .request()
                .method(REST.Method.POST)
                .endpoint(OAuthEndpoint.TOKEN_REVOKE)
                .buildBody(BodyBuilderType.OBJECT, obj -> {
                    obj.put(TokenRevocationRequest.TOKEN, getToken());
                    obj.put(TokenRevocationRequest.TOKEN_HINT, "access_token");
                })
                .execute()
                .thenApply(response -> {
                    if (response.getStatusCode() != 200)
                        throw response.toException();
                    return this;
                });
    }

    public final CompletableFuture<? extends AuthorizationUser> refreshAccessToken() {
        return invalidateToken().thenCompose(it -> getFromContext(REST.class)
                .orElseGet(() -> new REST(this))
                .request((ctx, dat) -> new OAuthAuthorization.AccessToken(ctx, dat.asObjectNode()))
                .method(REST.Method.POST)
                .endpoint(OAuthEndpoint.TOKEN)
                .buildBody(BodyBuilderType.OBJECT, obj -> obj.put("code", getAuthCode()))
                .execute$deserializeSingle()
                .thenApply(accessToken -> {
                    getExtractionReference(CURRENT_ACCESS_TOKEN).compute(refs -> {
                        if (refs == null)
                            return ReferenceList.of(accessToken.toUniNode());
                        refs.clear();
                        refs.add(accessToken.toUniNode());
                        return refs;
                    });
                    return this;
                }));
    }

    public final CompletableFuture<? extends AuthorizationUser> validateUserInfo() {
        return getFromContext(REST.class)
                .orElseGet(() -> new REST(this))
                .request()
                .method(REST.Method.GET)
                .endpoint(OAuthEndpoint.USER_INFO)
                .addHeader(CommonHeaderNames.AUTHORIZATION, getToken())
                .execute()
                .thenApply(response -> {
                    if (response.getStatusCode() != HTTPStatusCodes.OK)
                        throw response.toException();
                    UniNode data = response.getBody().into(Serializable::toUniNode);
                    updateFrom(data.asObjectNode());
                    return this;
                });
    }
}
