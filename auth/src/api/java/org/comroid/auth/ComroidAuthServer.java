package org.comroid.auth;

import org.apache.logging.log4j.LogManager;
import org.comroid.api.ContextualProvider;
import org.comroid.api.EMailAddress;
import org.comroid.api.Rewrapper;
import org.comroid.auth.service.Service;
import org.comroid.auth.user.User;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.UUID;

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

    public static boolean addUserToCache(final User user) {
        final UUID uuid = user.getUUID();
        getUser(uuid).compute(cached -> {
            if (cached == null)
                return user;
            cached.updateFrom(user);
            return cached;
        });
        return true;
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

    public static boolean hasUser(UUID uuid) {
        return userCache.containsKey(uuid);
    }

    public static Reference<Service> getService(final UUID uuid) {
        return serviceCache.getReference(uuid, true);
    }

    public static Reference<User> getUser(final UUID uuid) {
        return userCache.getReference(uuid, true);
    }

    public static Rewrapper<User> findUserByEmail(final EMailAddress address) {
        return getUsers()
                .filter(user -> user.getEMailAddress().equals(address))
                .findAny();
    }
}
