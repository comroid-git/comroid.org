package org.comroid.auth.service;

import org.comroid.auth.model.AuthEntity;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.webkit.oauth.client.Client;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.java_websocket.util.Base64;

import java.util.UUID;

public interface Service extends Resource, AuthEntity {
    @RootBind
    GroupBind<Service> Type = AuthEntity.Type.subGroup("org.comroid.auth-service");
    VarBind<Service, String, String, String> NAME
            = Type.createBind("name")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();

    @Override
    default String getName() {
        return assertion(NAME);
    }

    @Override
    String getSecret();

    @Override
    default String generateAccessToken(OAuthAuthorization authorization) {
        Client client = authorization.getClient();
        Resource resource = authorization.getResource();
        String code = String.format("%s-%s-%s", client.getUUID(), resource.getUUID(), UUID.randomUUID());
        code = Base64.encodeBytes(code.getBytes());
        return code;
    }
}
