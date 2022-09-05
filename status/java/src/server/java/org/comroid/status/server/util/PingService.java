package org.comroid.status.server.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.os.OS;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.entity.LocalStoredService;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

public class PingService extends DataContainerBase<Entity> implements Service {
    private static final Logger logger = LogManager.getLogger();
    @RootBind
    GroupBind<Service> GROUP = Service.Type.subGroup("local_service", LocalStoredService::new);
    private final String address;


    public PingService(ContextualProvider ctx, String name, String displayName, String address) {
        super(ctx, obj -> {
            obj.put(Service.NAME, name);
            obj.put(Service.DISPLAY_NAME, displayName);
            obj.put(Service.URL, "https://" + address);
        });
        this.address = address;
    }

    @Override
    public CompletableFuture<Status> requestStatus() {
        return CompletableFuture.supplyAsync(() -> {
                try {
                    int attempts = 4;
                    Runtime runtime = Runtime.getRuntime();
                    Process process = runtime.exec("ping -"+(OS.isWindows ? 'n' : 'c')+" "+attempts+" " + address);
                    process.waitFor();
                    try (
                            InputStreamReader isr = new InputStreamReader(process.getInputStream());
                            BufferedReader reader = new BufferedReader(isr);
                            ) {
                        Status status = reader.lines()
                                .map(String::toLowerCase)
                                .filter(s -> s.contains("time=") || s.contains("zeit="))
                                .count() == attempts ? Status.ONLINE : Status.OFFLINE;
                        logger.trace("Service " + getURL().orElseThrow(NoSuchElementException::new) + " is " + status.name());
                        return status;
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Could not ping service " + address, e);
                }
            })
            .thenApply(status -> {
                put(Service.STATUS, status);
                return status;
            });
    }

    @Override
    public CompletableFuture<Service> updateStatus(Status status) {
        throw new UnsupportedOperationException("Cannot update status of PingService");
    }
}
