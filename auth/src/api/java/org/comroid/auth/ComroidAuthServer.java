package org.comroid.auth;

import org.apache.logging.log4j.LogManager;
import org.comroid.api.ContextualProvider;
import org.comroid.auth.service.Service;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.Collection;
import java.util.UUID;

public final class ComroidAuthServer {
    public static final String URL_BASE = "https://auth.comroid.org";
    private static final RefMap<UUID, Service> serviceCache = new ReferenceMap<>();

    static {
        if (ContextualProvider.Base.ROOT.streamContextMembers(false).count() == 0)
            LogManager.getLogger().error("Warning: Root Context has not been properly initialized. Expect Errors");
    }

    public static Collection<Service> getServices() {
        return serviceCache.values();
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
