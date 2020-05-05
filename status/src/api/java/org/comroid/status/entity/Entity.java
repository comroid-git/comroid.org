package org.comroid.status.entity;

import org.comroid.status.ServerObject;
import org.comroid.status.StatusUpdater;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.UUID;

public interface Entity<SCOPE extends ServerObject> extends DataContainer<SCOPE> {
    default UUID getID() {
        return requireNonNull(Bind.ID);
    }

    default EntityType getType() {
        return requireNonNull(Bind.Type);
    }

    interface Bind {
        GroupBind<Entity<? super ServerObject>, ? super ServerObject> Root = new GroupBind<>(StatusUpdater.instance.getSerializationAdapter().join(), "entity");
        VarBind.TwoStage<String, UUID> ID = Root.bind2stage("id", UniValueNode.ValueType.STRING, UUID::fromString);
        VarBind.TwoStage<Integer, EntityType> Type = Root.bind2stage("type", UniValueNode.ValueType.INTEGER, EntityType::valueOf);
    }
}
