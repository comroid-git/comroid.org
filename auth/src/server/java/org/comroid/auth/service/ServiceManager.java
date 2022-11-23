package org.comroid.auth.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Rewrapper;
import org.comroid.auth.AuthServer;
import org.comroid.api.io.FileHandle;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.oauth.resource.ResourceProvider;

import java.io.File;
import java.util.UUID;
import java.util.stream.Stream;

public final class ServiceManager implements ContextualProvider.Underlying, ResourceProvider {
    public static final FileHandle DIR = org.comroid.auth.server.AuthServer.DATA.createSubDir("services");
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public Stream<Service> getServices() {
        return AuthServer.getServices().streamValues();
    }

    public ServiceManager(org.comroid.auth.server.AuthServer server) {
        this.context = server.plus("ServiceManager", this);

        Stream.of(DIR.listFiles())
                .filter(File::isDirectory)
                .filter(dir -> {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        UUID.fromString(dir.getName());
                        return true;
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .map(FileHandle::new)
                .map(dir -> new FileBasedService(this, dir))
                .forEach(AuthServer::addServiceToCache);
        logger.info("Loading finished; loaded {} services", AuthServer.getServices().size());
    }

    @Override
    public boolean hasResource(UUID uuid) {
        return AuthServer.hasService(uuid);
    }

    public Service createService(UniObjectNode initialData) {
        Service service = new FileBasedService(this, initialData);
        AuthServer.addServiceToCache(service);
        return service;
    }

    @Override
    public Rewrapper<Service> getResource(final UUID uuid) {
        return AuthServer.getService(uuid);
    }
}
