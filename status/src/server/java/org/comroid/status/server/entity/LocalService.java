package org.comroid.status.server.entity;

import org.comroid.common.Polyfill;
import org.comroid.common.func.Invocable;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

@Location(value = LocalService.class, fieldName = "GROUP")
public class LocalService extends DataContainerBase<DependenyObject> implements Service {
    @RootBind
    public static final GroupBind<Service, DependenyObject> GROUP = Bind.Root.subGroup(
            "local_service",
            Invocable.ofConstructor(Polyfill.uncheckedCast(LocalService.class))
    );

    public LocalService(StatusServer server, UniObjectNode data) {
        super(data, server);
    }

    public void setStatus(Status status) {
        //todo
    }

    @Override
    public Status getStatus() {
        //todo
    }
}
