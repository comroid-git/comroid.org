package org.comroid.auth;

import org.apache.logging.log4j.LogManager;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Rewrapper;
import org.comroid.auth.service.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ComroidAuthServer {
    public static final String URL_BASE = "https://auth.comroid.org";
    private static final Map<UUID, Service> serviceCache = new ConcurrentHashMap<>();

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

    public static void addServiceToCache(Service service) {
        serviceCache.put(service.getUUID(), service);
    }

    public static boolean hasService(UUID uuid) {
        return serviceCache.containsKey(uuid);
    }

    public static Rewrapper<Service> getService(final UUID uuid) {
        return () -> serviceCache.getOrDefault(uuid, null);
    }
}
