package org.comroid.status.server.entity;

import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.VariableCarrier;

public class LocalService extends VariableCarrier<StatusServer> implements Service<StatusServer> {
    public LocalService(StatusServer server, UniObjectNode data) {
        super(data, server);
    }
}
