package org.comroid.status.entity;

import org.comroid.api.Named;
import org.comroid.api.Specifiable;
import org.comroid.status.AdapterDefinition;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public interface Entity extends DataContainer<Entity>, Named, Specifiable<Entity> {
    default String getName() {
        return requireNonNull(Bind.Name);
    }

    interface Bind {
        GroupBind<Entity> Root
                = new GroupBind<>(AdapterDefinition.getInstance(), "entity");
        VarBind<Entity, String, String, String> Name
                = Root.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired(true)
                .build();
    }
}
