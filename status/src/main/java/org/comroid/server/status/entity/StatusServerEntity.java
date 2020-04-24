package org.comroid.server.status.entity;

import java.util.UUID;

import org.comroid.server.status.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.GroupBind;
import org.comroid.varbind.VarBind;
import org.comroid.varbind.VariableCarrier;

import static org.comroid.uniform.adapter.json.fastjson.FastJSONLib.fastJsonLib;

public abstract class StatusServerEntity extends VariableCarrier<StatusServer> {
    public static synchronized UUID nextID() {
        return UUID.randomUUID();
    }

    public final StatusServer getServer() {
        return getDependencyObject();
    }

    public UUID getID() {
        return requireNonNull(Bind.ID);
    }

    protected StatusServerEntity(StatusServer server, UniObjectNode initialData) {
        super(fastJsonLib, initialData, server);
    }

    public interface Bind {
        GroupBind                 Root = new GroupBind(fastJsonLib, "entity");
        VarBind.Duo<String, UUID> ID   = Root.bind2stage("id", UniValueNode.ValueType.STRING, UUID::fromString);
    }
}
