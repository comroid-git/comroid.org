package org.comroid.auth;

import org.apache.logging.log4j.LogManager;
import org.comroid.api.ContextualProvider;
import org.comroid.auth.service.Service;
import org.comroid.auth.user.User;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ComroidAuthServer {
    public static final String URL_BASE = "https://auth.comroid.org";
    private static final RefMap<UUID, Service> serviceCache = new ReferenceMap<>();
    private static final RefMap<UUID, User> userCache = new ReferenceMap<>();

    static {
        if (ContextualProvider.Base.ROOT.streamContextMembers(false).count() == 0)
            LogManager.getLogger().error("Warning: Root Context has not been properly initialized. Expect Errors");
    }

    public static RefContainer<UUID, Service> getServices() {
        return serviceCache.immutable();
    }

    public static RefContainer<UUID, User> getUsers() {
        return userCache.immutable();
    }

    private ComroidAuthServer() {
        throw new UnsupportedOperationException();
    }

    public static boolean addServiceToCache(final Service service) {
        final UUID uuid = service.getUUID();
        getService(uuid).compute(cached -> {
            if (cached == null)
                return service;
            cached.updateFrom(service);
            return cached;
        });
        return true;
    }

    public static boolean hasService(UUID uuid) {
        return serviceCache.containsKey(uuid);
    }

    public static Reference<Service> getService(final UUID uuid) {
        return serviceCache.getReference(uuid, true);
    }
}
