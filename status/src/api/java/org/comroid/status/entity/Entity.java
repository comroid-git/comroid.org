package org.comroid.status.entity;

import org.comroid.api.Specifiable;
import org.comroid.common.ref.Named;
import org.comroid.status.DependenyObject;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public interface Entity extends DataContainer<DependenyObject>, Named, Specifiable<Entity> {
    default String getName() {
        return requireNonNull(Bind.Name);
    }

    interface Bind {
        GroupBind<Entity, DependenyObject> Root
                = new GroupBind<>(DependenyObject.Adapters.SERIALIZATION_ADAPTER, "entity");
        VarBind.OneStage<String> Name
                = Root.bind1stage("name", ValueType.STRING);
    }
}
