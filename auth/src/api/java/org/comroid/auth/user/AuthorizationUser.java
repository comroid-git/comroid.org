package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.EMailAddress;
import org.comroid.auth.AuthServer;
import org.comroid.auth.model.AuthEntity;
import org.comroid.auth.rest.AuthEndpoint;
import org.comroid.auth.service.DataBasedService;
import org.comroid.auth.service.Service;
import org.comroid.api.info.MessageSupplier;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.ref.ReferenceMap;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private final RefMap<String, UniNode> serviceDataCache = new ReferenceMap<>();

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

        this.initialValidation = validateUserInfo()
                .exceptionally(t -> {
                    logger.fatal("Could not validate " + this, t);
                    invalidate();
                    return null;
                });
    }

    private static String createServiceDataKey(UUID serviceId, String storageName) {
        return serviceId + validateStorageName(storageName);
    }

    private static String validateStorageName(String storageName) {
        if (!storageName.matches(User.STORAGE_NAME_PATTERN))
            throw new IllegalArgumentException(String.format("Invalid storage name '%s'; must match %s",
                    storageName, User.STORAGE_NAME_PATTERN));
        return storageName;
    }

    private MessageSupplier unavailableMessage(String fieldName) {
        return MessageSupplier.format("Field '%s' unavailable; UserData was never validated", fieldName);
    }

    @Override
    public final boolean invalidate() {
        if ((!invalidation.isDone() && invalidation.complete(null)) | invalidateAuthorizationCode().join() == this) {
            onInvalidate();
            return true;
        }
        return false;
    }

    protected void onInvalidate() {
    }

    public final CompletableFuture<? extends AuthorizationUser> invalidateAuthorizationCode() {
        if (!isValid())
            return CompletableFuture.completedFuture(this);
        logger.debug("Invalidating Authorization Code");
        return upgrade(REST.class).request()
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
                    put(AUTHORIZATION_CODE, null);
                    invalidation.complete(null);
                    return this;
                });
    }

    public final CompletableFuture<? extends AuthorizationUser> invalidateToken() {
        if (!isValid())
            return CompletableFuture.completedFuture(this);
        logger.debug("Invalidating Authorization Token");
        return upgrade(REST.class)
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
                    put(AUTHORIZATION_CODE, null);
                    return this;
                });
    }

    public final CompletableFuture<? extends AuthorizationUser> refreshAccessToken() {
        logger.debug("Refreshing Authorization Token");
        return invalidateToken().thenCompose(it -> upgrade(REST.class)
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
        logger.debug("Validating identity of {}", this);
        return upgrade(REST.class)
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

    public final CompletableFuture<List<Service>> requestServices() {
        checkPermits(Permit.ADMIN);
        logger.debug("Requesting all Services");
        return upgrade(REST.class)
                .request()
                .method(REST.Method.GET)
                .endpoint(AuthEndpoint.SERVICES)
                .addHeader(CommonHeaderNames.AUTHORIZATION, getToken())
                .execute()
                .thenApply(response -> {
                    if (response.getStatusCode() != HTTPStatusCodes.OK)
                        throw response.toException();
                    return response.getBody()
                            .into(Serializable::toUniNode)
                            .stream()
                            .map(UniNode::asObjectNode)
                            .map(data -> new DataBasedService(this, data))
                            .filter(AuthServer::addServiceToCache)
                            .collect(Collectors.toList());
                }).thenApply(Collections::unmodifiableList);
    }

    public final boolean hasServiceData(UUID serviceId, String storageName) {
        return serviceDataCache.hasValue(createServiceDataKey(serviceId, storageName));
    }

    public final Ref<UniNode> getServiceData(UUID serviceId, String storageName) {
        return serviceDataCache.getReference(createServiceDataKey(serviceId, storageName), true);
    }

    public final CompletableFuture<UniNode> requestServiceData(final UUID serviceId, final String storageName) {
        checkPermits(Permit.STORAGE);
        validateStorageName(storageName);
        logger.debug("Requesting Service Storage {} of service {} ", storageName, serviceId);
        return upgrade(REST.class).request()
                .method(REST.Method.GET)
                .endpoint(AuthEndpoint.MODIFY_ACCOUNT_DATA_STORAGE, getUUID(), serviceId, storageName)
                .addHeader(CommonHeaderNames.AUTHORIZATION, getToken())
                .expect(true, HTTPStatusCodes.OK)
                .execute$deserializeSingle()
                .thenApply(data -> cacheServiceData(serviceId, storageName, data));
    }

    public final CompletableFuture<UniNode> updateServiceData(final UUID serviceId, final String storageName, Serializable data) {
        checkPermits(Permit.STORAGE);
        validateStorageName(storageName);
        logger.debug("Updating Service Storage {} of service {} ", storageName, serviceId);
        logger.trace("New Content: {}", data.toSerializedString());
        return upgrade(REST.class).request()
                .method(REST.Method.POST)
                .endpoint(AuthEndpoint.MODIFY_ACCOUNT_DATA_STORAGE, getUUID(), serviceId, storageName)
                .addHeader(CommonHeaderNames.AUTHORIZATION, getToken())
                .expect(true, HTTPStatusCodes.OK)
                .body(data)
                .execute$deserializeSingle()
                .thenApply(newData -> cacheServiceData(serviceId, storageName, newData));
    }

    private UniNode cacheServiceData(UUID serviceId, String storageName, final UniNode data) {
        return serviceDataCache.compute(createServiceDataKey(serviceId, storageName), (k, cached) -> {
            if (cached == null)
                return data;
            return cached.copyFrom(data);
        });
    }
}
