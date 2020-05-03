package org.comroid.status.entity;

import java.util.UUID;

import org.comroid.status.StatusUpdater;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VarCarrier;

public interface Entity<SCOPE> extends VarCarrier<SCOPE> {
    default UUID getID() {
        return requireNonNull(Bind.ID);
    }

    default EntityType getType() {
        return requireNonNull(Bind.Type);
    }

    interface Bind {
        GroupBind<Entity<?>>                Root = new GroupBind<>(StatusUpdater.instance.getSerializationAdapter()
                .join(), "entity");
        VarBind.Duo<String, UUID>        ID   = Root.bind2stage("id", UniValueNode.ValueType.STRING, UUID::fromString);
        VarBind.Duo<Integer, EntityType> Type = Root.bind2stage("type", UniValueNode.ValueType.INTEGER, EntityType::valueOf);
    }
}
