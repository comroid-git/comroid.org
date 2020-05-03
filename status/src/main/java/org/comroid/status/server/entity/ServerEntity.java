package org.comroid.status.server.entity;

import java.util.UUID;

import org.comroid.status.entity.Entity;
import org.comroid.status.entity.EntityType;
import org.comroid.status.server.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.VariableCarrier;

public abstract class ServerEntity extends VariableCarrier<StatusServer> implements Entity<StatusServer> {
    public ServerEntity(UniObjectNode initialData, StatusServer dependencyObject) {
        super(initialData, dependencyObject);
    }

    @Override
    public UUID getID() {
        return requireNonNull(Bind.ID);
    }

    @Override
    public EntityType getType() {
        return get(Bind.Type);
    }
}
