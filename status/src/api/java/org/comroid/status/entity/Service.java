package org.comroid.status.entity;

import org.comroid.common.func.Invocable;
import org.comroid.status.DependenyObject;
import org.comroid.status.StatusUpdater;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

@Location(Service.Bind.class)
public interface Service extends Entity {
    default Status getStatus() {
        return requireNonNull(Bind.Status);
    }

    default String getName() {
        return requireNonNull(Bind.Name);
    }

    @Override
    default EntityType getType() {
        return EntityType.SERVICE;
    }

    enum Status {
        UNKNOWN,

        ONLINE,
        MAINTENANCE,
        OFFLINE;

        public static Status valueOf(int value) {
            return values()[value];
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    interface Bind extends Entity.Bind {
        @RootBind
        GroupBind<Service, DependenyObject> Root = Entity.Bind.Root.subGroup("service", Invocable.ofConstructor(Basic.class));
        VarBind.OneStage<String> Name = Root.bind1stage("name", ValueType.STRING);
        VarBind.TwoStage<Integer, Status> Status = Root.bind2stage(
                "status",
                ValueType.INTEGER,
                Service.Status::valueOf
        );
    }

    final class Basic extends DataContainerBase<DependenyObject> implements Service {
        public Basic(StatusUpdater updater, UniObjectNode node) {
            super(node, updater);
        }
    }
}
