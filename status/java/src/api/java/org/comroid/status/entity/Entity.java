package org.comroid.status.entity;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.status.AdapterDefinition;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public interface Entity extends DataContainer<Entity>, Named {
    GroupBind<Entity> Type
            = new GroupBind<>(AdapterDefinition.getInstance(), "entity");
    VarBind<Entity, String, String, String> NAME
            = Type.createBind("name")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired(true)
            .build();

    default String getName() {
        return requireNonNull(NAME);
    }
}