package org.comroid.status.server.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.comroid.status.entity.EntityType;
import org.comroid.status.server.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VarBind.Location;
import org.comroid.varbind.VarBind.Root;
import org.comroid.varbind.VarCarrier;
import org.comroid.varbind.VariableCarrier;

@Location(Service.Bind.class)
public final class Service implements ServerEntity, VarCarrier.Underlying<StatusServer> {
    private final VarCarrier<StatusServer>        varCarrier;
    private final AtomicReference<Service.Status> status = new AtomicReference<>(Status.UNKNOWN);

    public Service(StatusServer server, UniObjectNode data) {
        this.varCarrier = new VariableCarrier<>(data, server, Service.class);
    }

    public AtomicReference<Status> getStatus() {
        return status;
    }

    public String getName() {
        return requireNonNull(Bind.Name);
    }

    @Override
    public UUID getID() {
        return requireNonNull(Bind.ID);
    }

    @Override
    public EntityType getType() {
        return requireNonNull(Bind.Type);
    }

    @Override
    public VarCarrier<StatusServer> getUnderlyingVarCarrier() {
        return varCarrier;
    }

    public String statusString() {
        return String.format("Service %s is %s", getName(), getStatus().get().toString());
    }

    public interface Bind extends ServerEntity.Bind {
        @Root GroupBind Root = ServerEntity.Bind.Root.subGroup("service");
        VarBind.Uno<String> Name = Root.bind1stage("name", UniValueNode.ValueType.STRING);
    }
}
