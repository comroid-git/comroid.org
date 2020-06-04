package org.comroid.status.server.entity;

import org.comroid.common.Polyfill;
import org.comroid.common.func.Invocable;
import org.comroid.common.ref.IntEnum;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.util.StatusContainer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.varbind.container.DataContainerBuilder;

import java.util.concurrent.atomic.AtomicReference;

@Location(value = LocalService.class, fieldName = "GROUP")
public final class LocalService extends DataContainerBase<DependenyObject> implements Service, StatusContainer {
    @RootBind
    public static final GroupBind<Service, DependenyObject> GROUP = Bind.Root.subGroup(
            "local_service",
            Invocable.ofConstructor(Polyfill.<Class<Service>>uncheckedCast(LocalService.class))
    );
    private final AtomicReference<Status> status;

    public LocalService(StatusServer server, UniObjectNode data) {
        super(data, server);

        this.status = new AtomicReference<>(wrap(Bind.Status).orElse(Status.UNKNOWN));
    }

    @Override
    public void setStatus(Status status) {
        this.status.set(status);
        put(Bind.Status, IntEnum::getValue, status);
    }

    @Override
    public Status getStatus() {
        return status.get();
    }

    public static final class Builder extends DataContainerBuilder<Builder, Service, DependenyObject> {
        public Builder() {
            super(Polyfill.uncheckedCast(LocalService.class), StatusServer.instance);
        }

        @Override
        protected Service mergeVarCarrier(DataContainer<DependenyObject> dataContainer) {
            return new OfUnderlying(dataContainer);
        }
    }

    private static final class OfUnderlying implements Service, StatusContainer, DataContainer.Underlying<DependenyObject> {
        private final DataContainer<DependenyObject> underlying;
        private final AtomicReference<Status> status;

        @Override
        public DataContainer<DependenyObject> getUnderlyingVarCarrier() {
            return underlying;
        }

        private OfUnderlying(DataContainer<DependenyObject> underlying) {
            this.underlying = underlying;

            this.status = new AtomicReference<>(underlying.wrap(Bind.Status).orElse(Status.UNKNOWN));
        }

        @Override
        public Status getStatus() {
            return status.get();
        }

        @Override
        public void setStatus(Status status) {
            this.status.set(status);
            put(Bind.Status, IntEnum::getValue, status);
        }
    }
}
