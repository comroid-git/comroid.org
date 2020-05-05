package org.comroid.status.entity;

import org.comroid.common.Polyfill;
import org.comroid.common.func.Invocable;
import org.comroid.status.ServerObject;
import org.comroid.status.StatusUpdater;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.ReBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

import java.util.concurrent.atomic.AtomicReference;

@Location(Service.Bind.class)
public interface Service<SPEC extends ServerObject> extends Entity<SPEC> {
    default AtomicReference<Service.Status> getStatus() {
        return requireNonNull(Bind.StatusRef);
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
        GroupBind<Service<? super ServerObject>, ? super ServerObject> Root = Entity.Bind.Root.subGroup("service", Invocable.ofConstructor(Polyfill.uncheckedCast(Basic.class)));
        VarBind.OneStage<String> Name = Root.bind1stage("name", UniValueNode.ValueType.STRING);
        VarBind.TwoStage<Integer, Status> Status = Root.bind2stage(
                "status",
                UniValueNode.ValueType.INTEGER,
                Service.Status::valueOf
        );
        ReBind.Duo<Service.Status, AtomicReference<Status>> StatusRef = Status.rebindSimple(AtomicReference::new);
    }

    final class Basic extends DataContainerBase<StatusUpdater> implements Service<StatusUpdater> {
        public Basic(StatusUpdater updater, UniObjectNode node) {
            super(node, updater);
        }
    }
}
