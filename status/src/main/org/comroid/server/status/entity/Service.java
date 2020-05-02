package org.comroid.server.status.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.comroid.server.status.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind.Location;
import org.comroid.varbind.VarBind.Root;
import org.comroid.varbind.VarCarrier;
import org.comroid.varbind.VariableCarrier;

@Location(Service.Bind.class)
public final class Service implements Entity, VarCarrier.Underlying<StatusServer> {
    private final VarCarrier<StatusServer>        varCarrier;
    private final AtomicReference<Service.Status> status = new AtomicReference<>(Status.UNKNOWN);

    public Service(StatusServer server, UniObjectNode data) {
        this.varCarrier = new VariableCarrier<>(data, server, Service.class);
    }

    public AtomicReference<Status> getStatus() {
        return status;
    }

    @Override
    public UUID getID() {
        return requireNonNull(Bind.ID);
    }

    @Override
    public Type getType() {
        return requireNonNull(Bind.Type);
    }

    @Override
    public VarCarrier<StatusServer> getUnderlyingVarCarrier() {
        return varCarrier;
    }

    public interface Bind extends Entity.Bind {
        @Root GroupBind Root = Entity.Bind.Root.subGroup("service");
    }

    public enum Status {
        UNKNOWN,

        ONLINE,
        MAINTENANCE,
        OFFLINE;

        public static Status valueOf(int value) {
            return values()[value];
        }
    }
}
