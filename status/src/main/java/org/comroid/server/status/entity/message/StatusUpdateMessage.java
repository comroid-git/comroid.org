package org.comroid.server.status.entity.message;

import org.comroid.server.status.StatusServer;
import org.comroid.server.status.entity.StatusServerEntity;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;

import static org.comroid.varbind.VarBind.Location;
import static org.comroid.varbind.VarBind.Root;

@Location(StatusUpdateMessage.Bind.class)
public final class StatusUpdateMessage extends StatusServerEntity {
    public StatusUpdateMessage(StatusServer server, UniObjectNode initialData) {
        super(server, initialData);
    }

    public final String getAppName() {
        return requireNonNull(Bind.AppName);
    }

    public interface Bind extends StatusServerEntity.Bind {
        @Root GroupBind Root = StatusServerEntity.Bind.Root.subGroup("message_status_update");
        VarBind.Uno<String> AppName = Root.bind1stage("app_name", UniValueNode.ValueType.STRING);
    }
}
