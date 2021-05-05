package org.comroid.auth.model;

import org.comroid.api.UUIDContainer;
import org.comroid.auth.service.Service;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.UUID;

public interface AuthEntity extends DataContainer<AuthEntity>, UUIDContainer {
    @RootBind
    GroupBind<AuthEntity> Type = new GroupBind<>("org.comroid.auth-entity");
    VarBind<AuthEntity, String, UUID, UUID> ID
            = Type.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .onceEach()
            .setRequired()
            .build();

    default UUID getUUID() {
        return assertion(ID);
    }
}
