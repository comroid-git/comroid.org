package org.comroid.auth.user;

import com.sun.net.httpserver.Headers;
import org.comroid.api.os.OS;
import org.comroid.auth.model.PermitCarrier;
import org.comroid.auth.server.AuthConnection;
import org.comroid.auth.server.AuthServer;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.model.CookieProvider;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.COOKIE;
import static org.comroid.restless.HTTPStatusCodes.UNAUTHORIZED;

public final class UserSession implements PermitCarrier, CookieProvider {
    public static final String COOKIE_PREFIX = "org.comroid.auth";
    public static final String NULL_COOKIE = CookieProvider.assembleCookie(COOKIE_PREFIX, "null");
    public final Ref<AuthConnection> connection = Reference.create();
    private final UserAccount account;
    private final String cookie;

    public UserAccount getAccount() {
        return account;
    }

    @Override
    public String getPlainCookie() {
        return cookie;
    }

    @Override
    public @Nullable Duration getDefaultCookieMaxAge() {
        return Duration.ofHours(6);
    }

    @Override
    public @Nullable String getDefaultCookieDomain() {
        return OS.isWindows ? null : ".comroid.org";
    }

    @Override
    public @Nullable String getDefaultCookiePath() {
        return "/";
    }

    @Override
    public String getCookiePrefix() {
        return COOKIE_PREFIX;
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

    UserSession(UserAccount account) {
        this.account = account;
        this.cookie = generateCookie();
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

    private String generateCookie() {
        return String.format("%s-%s", account.getUUID(), UUID.randomUUID());
    }
}
