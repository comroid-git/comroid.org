package org.comroid.auth.user;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class UserSession {
    private final String cookie;
    private final UserAccount account;

    public String getCookie() {
        return cookie;
    }

    public UserAccount getAccount() {
        return account;
    }

    UserSession(UserAccount account) {
        String str = String.format("%s-%s-%s", UUID.randomUUID(), account.getUUID(), UUID.randomUUID());
        byte[] encoded = Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8));
        this.cookie = new String(encoded);
        this.account = account;
    }
}
