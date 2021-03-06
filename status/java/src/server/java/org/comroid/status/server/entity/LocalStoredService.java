package org.comroid.status.server.entity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.ref.Reference;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.auth.TokenCore;
import org.comroid.uniform.node.UniNode;
import org.comroid.varbind.container.DataContainerBase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LocalStoredService extends DataContainerBase<Entity> implements LocalService {
    private static final Logger logger = LogManager.getLogger();
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
        put(STATUS, status);
    }

    public LocalStoredService(ContextualProvider context, UniNode data) {
        super(StatusServer.instance, data.asObjectNode());

        this.status = new AtomicReference<>(wrap(STATUS).orElse(Status.UNKNOWN));
        this.tokenFile = StatusServer.TOKEN_DIR.createSubFile(getName() + ".token");

        this.token = new AtomicReference<>(null);
        if (!tokenFile.exists())
            overwriteTokenFile();
        else token.set(tokenFile.getContent());
    }

    @Override
    public CompletableFuture<Service> updateStatus(Status status) {
        setStatus(status);
        return CompletableFuture.completedFuture(this);
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
        spm.set(null);
    }

    private final class StatusPollManager {
        private final Reference<Status> state = Reference.create();

        private StatusPollManager(int expected, int timeout) {
            executor.schedule(this::expire, expected, TimeUnit.SECONDS);
            executor.schedule(this::timeout, timeout, TimeUnit.SECONDS);
        }

        private boolean complete(Status newStatus) {
            synchronized (spm) {
                if (state.isNonNull() && !state.contentEquals(Status.NOT_RESPONDING))
                    return false;
                state.set(newStatus);
                setStatus(newStatus);
                return true;
            }
        }

        private void expire() {
            if (complete(Status.NOT_RESPONDING))
                logger.warn("Service {} is not responding within timeout ...", getDisplayName());
        }

        private void timeout() {
            if (complete(Status.CRASHED))
                logger.warn("Service {} timed out!", getDisplayName());
        }
    }
}
