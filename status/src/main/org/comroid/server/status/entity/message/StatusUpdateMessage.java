package org.comroid.server.status.entity.message;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.comroid.server.status.StatusServer;
import org.comroid.server.status.entity.StatusServerEntity;
import org.comroid.server.status.entity.service.Service;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.ReBind;
import org.comroid.varbind.VarBind;

import static org.comroid.varbind.VarBind.Location;
import static org.comroid.varbind.VarBind.Root;

@Location(StatusUpdateMessage.Bind.class)
public final class StatusUpdateMessage extends StatusServerEntity {
    public String getAppName() {
        return requireNonNull(Bind.AppName);
    }

    public UUID getTargetID() {
        return getID();
    }

    public StatusUpdateMessage(StatusServer server, UniObjectNode initialData) {
        super(Type.MESSAGE, server, initialData);
    }

    public interface Bind extends StatusServerEntity.Bind {
        @Root GroupBind Root = StatusServerEntity.Bind.Root.subGroup("message_status_update");
        VarBind.Uno<String>                     AppName = Root.bind1stage("app_name", UniValueNode.ValueType.STRING);
        ReBind.Dep<UUID, StatusServer, Service> Service = ID.rebindDependent(Root,
                (id, server) -> server.getServiceByID(id)
                        .orElseThrow(NoSuchElementException::new)
        );
    }
}
