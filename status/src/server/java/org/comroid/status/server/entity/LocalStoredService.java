package org.comroid.status.server.entity;

import org.comroid.common.Polyfill;
import org.comroid.common.ref.IntEnum;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.varbind.container.DataContainerBuilder;

import java.util.concurrent.atomic.AtomicReference;

public final class LocalStoredService extends DataContainerBase<DependenyObject> implements LocalService {
    private final AtomicReference<Status> status;

    public LocalStoredService(StatusServer server, UniObjectNode data) {
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
            super(Polyfill.uncheckedCast(LocalStoredService.class), StatusServer.instance);
        }

        @Override
        protected Service mergeVarCarrier(DataContainer<DependenyObject> dataContainer) {
            return new OfUnderlying(dataContainer);
        }
    }

    private static final class OfUnderlying implements LocalService, DataContainer.Underlying<DependenyObject> {
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
