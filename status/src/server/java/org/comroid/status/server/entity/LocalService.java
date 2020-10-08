package org.comroid.status.server.entity;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.status.entity.Service;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;

import java.util.concurrent.CompletableFuture;

@Location(value = LocalStoredService.class, fieldName = "GROUP")
public interface LocalService extends Service {
    @RootBind
    GroupBind<Service> GROUP = Bind.Root.subGroup(
            "local_service",
            Invocable.ofConstructor(Polyfill.<Class<Service>>uncheckedCast(LocalStoredService.class))
    );

    @Override
    Status getStatus();

    @Override
    default CompletableFuture<Status> requestStatus() {
        return CompletableFuture.completedFuture(getStatus());
    }

    void setStatus(Status status);

    String getToken();

    String regenerateToken();

    void receivePoll(Status newStatus, int expected, int timeout);

    void discardPoll(Status newStatus);
}
