package org.comroid.auth.user;

import com.sun.net.httpserver.Headers;
import org.comroid.api.UncheckedCloseable;
import org.comroid.api.os.OS;
import org.comroid.auth.model.PermitCarrier;
import org.comroid.auth.server.AuthConnection;
import org.comroid.auth.server.AuthServer;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.oauth.model.ValidityStage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.COOKIE;
import static org.comroid.restless.HTTPStatusCodes.UNAUTHORIZED;

public final class UserSession implements PermitCarrier, ValidityStage, UncheckedCloseable, Serializable {
    public static final String COOKIE_PREFIX = "org.comroid.auth";
    public static final int MAX_COOKIE_AGE_SECONDS = 3600;
    public static final String NULL_COOKIE = wrapCookie("null");
    public final Ref<AuthConnection> connection = Reference.create();
    private final CompletableFuture<Void> invalidation = new CompletableFuture<>();
    private final UserAccount account;
    private final String cookie;
    private final long expiry;

    public UserAccount getAccount() {
        return account;
    }

    public String getPlainCookie() {
        return cookie;
    }

    public String getCookie() {
        return wrapCookie(cookie);
    }

    public UniObjectNode getSessionData() {
        UniObjectNode data = AuthServer.SERI_LIB.createObjectNode();

        UniObjectNode account = this.account.toObjectNode(data.putObject("account"));
        account.remove("org/comroid/webkit/oauth");

        return data;
    }

    @Override
    public Set<Permit> getPermits() {
        return getAccount().getPermits();
    }

    @Override
    public boolean isValid() {
        return !invalidation.isDone() && !invalidation.isCancelled() && !invalidation.isCompletedExceptionally() && System.currentTimeMillis() < expiry;
    }

    UserSession(UserAccount account) {
        this.account = account;
        this.cookie = generateCookie();
        this.expiry = Instant.now().plus(MAX_COOKIE_AGE_SECONDS, ChronoUnit.SECONDS).toEpochMilli();
    }

    private UserSession(UserAccount account, String cookie, long expiry) {
        this.account = account;
        this.cookie = cookie;
        this.expiry = expiry;
    }

    public static String wrapCookie(String cookie) {
        return String.format("%s=%s; Max-Age=%d; Path=/%s", UserSession.COOKIE_PREFIX, cookie, MAX_COOKIE_AGE_SECONDS, OS.current == OS.UNIX ? "; Domain=.comroid.org" : "");
    }

    public static UserSession findSession(REST.Header.List headers) {
        return findSession(findCookie(headers.toJavaHeaders()));
    }

    public static UserSession findSession(Headers headers) {
        return findSession(findCookie(headers));
    }

    public static UserSession findSession(String cookie) {
        return Stream.of(cookie)
                .filter(Objects::nonNull)
                .map(str -> str.split("; "))
                .flatMap(Stream::of)
                .filter(UserSession::isAppCookie)
                .map(str -> str.substring(UserSession.COOKIE_PREFIX.length() + 1))
                .map(c -> {
                    try {
                        return AuthServer.instance.getUserManager().findSession(c);
                    } catch (Throwable ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED, "Invalid Session Cookie"));
    }

    private static String findCookie(Headers headers) {
        if (!headers.containsKey(COOKIE))
            return "";
        return headers.get(COOKIE)
                .stream()
                .flatMap(str -> Stream.of(str.split("[,;\\s]")).filter(s -> !s.isEmpty()))
                .filter(UserSession::isAppCookie)
                .findAny()
                .orElse("");
    }

    public static boolean isAppCookie(String fullCookie) {
        return fullCookie.indexOf(COOKIE_PREFIX) == 0;
    }

    public static UserSession parse(UserManager userManager, UniObjectNode data) {
        UUID accountId = data.use("accountId")
                .map(UniNode::asString)
                .map(UUID::fromString)
                .assertion("Account ID not found");
        String cookie = data.get("cookie").asString();
        long expiry = data.get("expiry").asLong();

        UserAccount account = userManager.getUser(accountId)
                .assertion("User not found: " + accountId);

        return new UserSession(account, cookie, expiry);
    }

    @Override
    public boolean invalidate() {
        return invalidation.complete(null);
    }

    private String generateCookie() {
        return String.format("%s-%s", account.getUUID(), UUID.randomUUID());
    }

    @Override
    public void close() {
        invalidate();
    }

    @Override
    public UniNode toUniNode() {
        final Context context = account.upgrade(Context.class);
        final UniObjectNode data = context.createObjectNode();

        data.put("accountId", account.getUUID());
        data.put("cookie", cookie);
        data.put("expiry", expiry);

        return data;
    }
}
