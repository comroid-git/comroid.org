package org.comroid.status.server.entity;

import org.comroid.status.entity.Service;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;

import java.util.concurrent.CompletableFuture;

public interface LocalService extends Service {
    @RootBind
    GroupBind<Service> GROUP = Service.Type.subGroup("local_service", LocalStoredService::new);

    @Override
    Status getStatus();

    void setStatus(Status status);

    String getToken();

    @Override
    default CompletableFuture<Status> requestStatus() {
        return CompletableFuture.completedFuture(getStatus());
    }

    String regenerateToken();

    void receivePoll(Status newStatus, int expected, int timeout);

    void discardPoll(Status newStatus);
}
