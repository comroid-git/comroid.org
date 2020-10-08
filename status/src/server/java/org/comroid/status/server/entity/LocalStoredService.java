package org.comroid.status.server.entity;

import org.comroid.api.IntEnum;
import org.comroid.api.Polyfill;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.ref.Reference;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.TokenCore;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.varbind.container.DataContainerBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class LocalStoredService extends DataContainerBase<Entity> implements LocalService {
    private final AtomicReference<Status> status;
    private final AtomicReference<String> token;
    private final FileHandle tokenFile;
    private final ScheduledExecutorService executor = StatusServer.instance.getThreadPool();
    private final Reference<StatusPollManager> spm = Reference.create();

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
        tokenFile.setContent(newToken);
        token.set(newToken);

        return newToken;
    }

    @Override
    public String regenerateToken() {
        return overwriteTokenFile();
    }

    @Override
    public void receivePoll(Status newStatus, int expected, int timeout) {
        discardPoll(newStatus);
        spm.set(new StatusPollManager(expected, timeout));
    }

    @Override
    public void discardPoll(final Status newStatus) {
        spm.ifPresentOrElse(spm -> spm.complete(newStatus), () -> setStatus(newStatus));
    }

    private final class StatusPollManager {
        private final Reference<Status> state = Reference.create();

        private StatusPollManager(int expected, int timeout) {
            executor.schedule(this::expire, expected, TimeUnit.SECONDS);
            executor.schedule(this::timeout, timeout, TimeUnit.SECONDS);
        }

        private boolean complete(Status newStatus) {
            synchronized (spm) {
                if (state.isNonNull())
                    return false;
                state.set(newStatus);
                setStatus(newStatus);
                return true;
            }
        }

        private void expire() {
            if (complete(Status.NOT_RESPONDING))
                StatusServer.logger.at(Level.WARNING)
                        .log("Service {} is not responding within timeout ...", getDisplayName());
        }

        private void timeout() {
            if (complete(Status.CRASHED))
                StatusServer.logger.at(Level.SEVERE)
                        .log("Service {} timed out!", getDisplayName());
        }
    }
}
