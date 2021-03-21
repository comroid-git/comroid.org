package org.comroid.auth.user;

import com.sun.net.httpserver.Headers;
import org.comroid.auth.server.AuthServer;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.COOKIE;
import static org.comroid.restless.HTTPStatusCodes.UNAUTHORIZED;

public final class UserSession {
    public static final String COOKIE_PREFIX = "org.comroid.auth";
    private final UserAccount account;
    private final String cookie;

    public UserAccount getAccount() {
        return account;
    }

    public String getCookie() {
        return cookie;
    }

    public UniObjectNode getSessionData() {
        UniObjectNode data = FastJSONLib.fastJsonLib.createObjectNode();

        account.toObjectNode(data.putObject("account")).remove("password");

        return data;
    }

    UserSession(UserAccount account) {
        this.account = account;
        this.cookie = generateCookie();
    }

    public static UserSession findSession(Headers headers) {
        String[] cookies = headers.getFirst(COOKIE).split("; ");
        return Stream.of(cookies)
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
