package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.common.io.FileHandle;
import org.comroid.common.ref.Reference;
import org.comroid.dreadpool.ThreadPool;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.jdk.JavaHttpAdapter;
import org.comroid.restless.adapter.okhttp.v3.OkHttp3Adapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.rest.ServerEndpoints;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.FileCache;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class StatusServer implements DependenyObject {
    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static final String PATH_BASE = "/home/comroid/srv_status/"; // server path base
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final FileHandle CACHE_FILE = new FileHandle(PATH_BASE + "data/cache.json");
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static StatusServer instance;
    private final ThreadPool threadPool;
    private final FileCache<String, Entity, DependenyObject> entityCache;
    private final REST<StatusServer> rest;
    private final RestServer server;

    public FastJSONLib getSerializationLibrary() {
        return FastJSONLib.fastJsonLib;
    }

    public final Cache<String, Entity> getEntityCache() {
        return entityCache;
    }

    public final RestServer getServer() {
        return server;
    }

    public final ThreadPool getThreadPool() {
        return threadPool;
    }

    private StatusServer(InetAddress host, int port) throws IOException {
        Adapters.HTTP_ADAPTER = new OkHttp3Adapter();
        Adapters.SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;

        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        this.rest = new REST<>(DependenyObject.Adapters.HTTP_ADAPTER, DependenyObject.Adapters.SERIALIZATION_ADAPTER, threadPool, this);
        this.entityCache = new FileCache<>(getSerializationLibrary(), Entity.Bind.Name, CACHE_FILE, 250, this);
        this.server = new RestServer(this.rest, host, port, ServerEndpoints.values());
    }

    public static void main(String[] args) throws IOException {
        logger.at(Level.INFO).log(
                "Starting comroid Status Server..."
        );
        instance = new StatusServer(InetAddress.getByAddress(new byte[]{0,0,0,0}), PORT);
        DiscordBot.INSTANCE.supplyToken(instance, args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(instance::shutdown));
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
