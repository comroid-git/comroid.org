package org.comroid.status.server;

import com.sun.net.httpserver.HttpServer;
import org.comroid.common.func.Invocable;
import org.comroid.common.io.FileHandle;
import org.comroid.common.ref.Reference;
import org.comroid.dreadpool.ThreadPool;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.FileCache;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

public class StatusServer implements DependenyObject {
    public static final String PATH_BASE = "/home/comroid/srv_status/"; // server path base
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final FileHandle CACHE_FILE = new FileHandle(PATH_BASE + "data/cache.json");
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static StatusServer instance;
    private final HttpServer server;
    private final ThreadPool threadPool;
    private final FileCache<UUID, Entity, DependenyObject> entityCache;

    private StatusServer(InetAddress host, int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(host, port), port);
        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        this.entityCache = new FileCache<>(getSerializationLibrary(), Entity.Bind.ID, CACHE_FILE, 250, this);

        server.setExecutor(threadPool);
        server.createContext("/", exchange -> ContextHandler.sortExchange(this, exchange));

        this.server.start();
    }

    public static void main(String[] args) throws IOException {
        instance = new StatusServer(InetAddress.getLocalHost(), PORT);
        DiscordBot.INSTANCE.supplyToken(instance, args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(instance::shutdown));
    }

    public FastJSONLib getSerializationLibrary() {
        return FastJSONLib.fastJsonLib;
    }

    public final Cache<UUID, Entity> getEntityCache() {
        return entityCache;
    }

    public final HttpServer getServer() {
        return server;
    }

    public final ThreadPool getThreadPool() {
        return threadPool;
    }

    public final Optional<Service> getServiceByID(UUID id) {
        return entityCache.stream(it -> it.equals(id))
                .findAny()
                .map(Service.class::cast);
    }

    public final Optional<Service> getServiceByName(String name) {
        return entityCache.stream()
                .filter(ref -> !ref.isNull())
                .filter(ref -> ref.process()
                        .test(Service.class::isInstance))
                .map(Reference::requireNonNull)
                .map(Service.class::cast)
                .filter(service -> service.getName()
                        .equals(name))
                .findFirst();
    }

    private void shutdown() {
        entityCache.disposeThrow();
    }
}
