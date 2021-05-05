package org.comroid.auth.user;

import org.comroid.api.EMailAddress;
import org.comroid.auth.model.AuthEntity;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.webkit.oauth.client.Client;

import java.util.UUID;

public interface User extends Client, AuthEntity {
    GroupBind<User> Type = AuthEntity.Type.subGroup("org.comroid.auth-user");
    VarBind<User, String, UUID, UUID> ID
            = Type.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .build();
    VarBind<User, String, String, String> USERNAME
            = Type.createBind("username")
            .extractAs(StandardValueType.STRING)
            .build();
    VarBind<User, String, EMailAddress, EMailAddress> EMAIL
            = Type.createBind("email")
            .extractAs(StandardValueType.STRING)
            .andRemap(EMailAddress::parse)
            .build();

    @Override
    default UUID getUUID() {
        return assertion(ID);
    }

    default String getUsername() {
        return assertion(USERNAME);
    }

    default EMailAddress getEMailAddress() {
        return assertion(EMAIL);
    }
}
