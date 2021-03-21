package org.comroid.auth.user;

import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;

import java.util.UUID;

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

        account.toObjectNode(data.putObject("account"));

        return data;
    }

    UserSession(UserAccount account) {
        this.account = account;
        this.cookie = generateCookie();
    }

    public static boolean isAppCookie(String fullCookie) {
        return fullCookie.indexOf(COOKIE_PREFIX) == 0;
    }

    private String generateCookie() {
        return String.format("%s-%s", account.getUUID(), UUID.randomUUID());
    }
}
