package org.comroid.status.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.comroid.common.func.Invocable;
import org.comroid.status.StatusUpdater;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.ReBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VariableCarrier;

@VarBind.Location(Service.Bind.class)
public interface Service<SPEC> extends Entity<SPEC> {
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

    interface Bind extends Entity.Bind {
        @VarBind.Root GroupBind<Service<?>> Root = Entity.Bind.Root.subGroup("service", Invocable.ofConstructor(Basic.class));
        VarBind.Uno<String>                                 Name      = Root.bind1stage("name", UniValueNode.ValueType.STRING);
        VarBind.Duo<Integer, Status>                        Status    = Root.bind2stage(
                "status",
                UniValueNode.ValueType.INTEGER,
                Service.Status::valueOf
        );
        ReBind.Duo<Service.Status, AtomicReference<Status>> StatusRef = Status.rebindSimple(AtomicReference::new);
    }

    enum Status {
        UNKNOWN,

        ONLINE,
        MAINTENANCE,
        OFFLINE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public static Status valueOf(int value) {
            return values()[value];
        }
    }

    final class Basic extends VariableCarrier<StatusUpdater> implements Service<StatusUpdater> {
        public Basic(StatusUpdater updater, UniObjectNode node) {
            super(node, updater);
        }
    }
}
