package org.comroid.status.server.entity;

import org.comroid.api.IntEnum;
import org.comroid.api.Polyfill;
import org.comroid.common.io.FileHandle;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.TokenCore;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.varbind.container.DataContainerBuilder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicReference;

public class LocalStoredService extends DataContainerBase<Entity> implements LocalService {
    private final AtomicReference<Status> status;
    private final AtomicReference<String> token;
    private final FileHandle tokenFile;

    @Override
    public String getToken() {
        return token.get();
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

    public LocalStoredService(UniObjectNode data) {
        super(data);

        this.status = new AtomicReference<>(wrap(Bind.Status).orElse(Status.UNKNOWN));
        this.tokenFile = StatusServer.TOKEN_DIR.createSubFile(getName() + ".token");

        this.token = new AtomicReference<>(null);
        if (!tokenFile.exists())
            overwriteTokenFile();
        else token.set(tokenFile.getContent());
    }

    private synchronized String overwriteTokenFile() {
        if (!((!tokenFile.exists() || tokenFile.delete()) && tokenFile.validateExists()))
            throw new RuntimeException("Could not replace old Token file");

        final String newToken = TokenCore.generate(getName());

        try (
                FileOutputStream fos = new FileOutputStream(tokenFile);
                OutputStreamWriter osw = new OutputStreamWriter(fos)
        ) {
            osw.write(newToken);
            osw.flush();
            fos.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not write token", e);
        } finally {
            token.set(newToken);
        }

        return newToken;
    }

    @Override
    public void regenerateToken() {
        overwriteTokenFile();
    }

    @Override
    public void receivePoll(Status newStatus, int expected, int timeout) {

    }

    public static final class Builder extends DataContainerBuilder<Builder, Entity> {
        public Builder() {
            super(LocalStoredService.class);
        }

        @Override
        protected Entity mergeVarCarrier(DataContainer<? super Entity> dataContainer) {
            return new OfUnderlying(Polyfill.uncheckedCast(dataContainer));
        }
    }

    private static final class OfUnderlying extends LocalStoredService
            implements LocalService, DataContainer.Underlying<Entity> {
        private final DataContainer<Entity> underlying;

        @Override
        public DataContainer<Entity> getUnderlyingVarCarrier() {
            return underlying;
        }

        private OfUnderlying(DataContainer<Entity> underlying) {
            super(null);

            this.underlying = underlying;
        }
    }
}
