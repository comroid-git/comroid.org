package org.comroid.status.entity;

import org.comroid.api.Named;
import org.comroid.status.AdapterDefinition;
import org.comroid.status.StatusConnection;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public interface Entity extends DataContainer<Entity>, Named {
    default String getName() {
        return requireNonNull(Bind.Name);
    }

    interface Bind {
        GroupBind<Entity> Root
                = new GroupBind<>(StatusConnection.SERIALIZER, "entity");
        VarBind<Entity, String, String, String> Name
                = Root.createBind("name")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired(true)
                .build();
    }
}
