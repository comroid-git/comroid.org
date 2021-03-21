package org.comroid.auth.user;

import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;

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

    public UniObjectNode getSessionData() {
        UniObjectNode data = FastJSONLib.fastJsonLib.createObjectNode();

        account.toObjectNode(data.putObject("account"));

        return data;
    }

    UserSession(UserAccount account) {
        String str = String.format("%s-%s-%s", UUID.randomUUID(), account.getUUID(), UUID.randomUUID());
        byte[] encoded = Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8));
        this.cookie = new String(encoded);
        this.account = account;
    }
}
