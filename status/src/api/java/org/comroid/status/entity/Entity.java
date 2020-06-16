package org.comroid.status.entity;

import org.comroid.api.Specifiable;
import org.comroid.common.ref.Named;
import org.comroid.status.DependenyObject;
import org.comroid.status.StatusConnection;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public interface Entity extends DataContainer<DependenyObject>, Named, Specifiable<Entity> {
    default String getName() {
        return requireNonNull(Bind.Name);
    }

    default StatusConnection requireConnection() {
        final DependenyObject dependent = getDependent();

        if (dependent instanceof StatusConnection)
            return (StatusConnection) dependent;

        throw new IllegalStateException("Dependent is not of type StatusConnection");
    }

    interface Bind {
        GroupBind<Entity, DependenyObject> Root
                = new GroupBind<>(DependenyObject.Adapters.SERIALIZATION_ADAPTER, "entity");
        VarBind<String, DependenyObject, String, String> Name
                = Root.createBind("name")
                .extractAs(ValueType.STRING)
                .asIdentities()
                .onceEach()
                .build();
    }
}
