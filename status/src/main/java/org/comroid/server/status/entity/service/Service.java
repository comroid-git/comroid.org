package org.comroid.server.status.entity.service;

import org.comroid.server.status.StatusServer;
import org.comroid.server.status.entity.StatusServerEntity;
import org.comroid.uniform.node.UniObjectNode;

public class Service extends StatusServerEntity {
    protected Service(StatusServer server, UniObjectNode initialData) {
        super(Type.SERVICE, server, initialData);
    }
}
