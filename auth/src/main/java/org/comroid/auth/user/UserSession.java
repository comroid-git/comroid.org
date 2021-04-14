package org.comroid.auth.user;

import com.sun.net.httpserver.Headers;
import org.comroid.api.os.OS;
import org.comroid.auth.server.AuthConnection;
import org.comroid.auth.server.AuthServer;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.COOKIE;
import static org.comroid.restless.HTTPStatusCodes.UNAUTHORIZED;

public final class UserSession {
    public static final String COOKIE_PREFIX = "org.comroid.auth";
    public static final String NULL_COOKIE = wrapCookie("null");
    public final Ref<AuthConnection> connection = Reference.create();
    private final UserAccount account;
    private final String cookie;

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
        account.remove("login");
        account.compute("email", (k, str) -> ((UniNode) str).asString().replace("%40", "@"));

        return data;
    }

    UserSession(UserAccount account) {
        this.account = account;
        this.cookie = generateCookie();
    }

    public static String wrapCookie(String cookie) {
        return String.format("%s=%s%s", UserSession.COOKIE_PREFIX, cookie, OS.current == OS.UNIX ? "; Domain=.comroid.org; Path=/" : "");
    }

    public static UserSession findSession(REST.Header.List headers) {
        return findSession(headers.getFirst(COOKIE));
    }

    public static UserSession findSession(Headers headers) {
        return findSession(headers.getFirst(COOKIE));
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
                .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED));
    }

    public static boolean isAppCookie(String fullCookie) {
        return fullCookie.indexOf(COOKIE_PREFIX) == 0;
    }

    private String generateCookie() {
        return String.format("%s-%s", account.getUUID(), UUID.randomUUID());
    }
}
