package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.common.io.FileHandle;
import org.comroid.dreadpool.ThreadPool;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.okhttp.v3.OkHttp3Adapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.rest.ServerEndpoints;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.FileCache;
import org.comroid.varbind.bind.VarBind;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.logging.Level;

public class StatusServer implements DependenyObject {
    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static final FileHandle PATH_BASE = new FileHandle("/home/comroid/srv_status/", true); // server path base
    public static final FileHandle DATA_DIR = PATH_BASE.createSubDir("data");
    public static final FileHandle CACHE_FILE = DATA_DIR.createSubFile("cache.json");
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static StatusServer instance;
    private final ThreadPool threadPool;
    private final FileCache<String, Entity, DependenyObject> entityCache;
    private final REST<StatusServer> rest;
    private final RestServer server;

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

        logger.at(Level.INFO).log("Initialized Adapters");

        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        logger.at(Level.INFO).log("ThreadPool created: %s", threadPool);

        this.rest = new REST<>(DependenyObject.Adapters.HTTP_ADAPTER, DependenyObject.Adapters.SERIALIZATION_ADAPTER, threadPool, this);
        logger.at(Level.INFO).log("REST Client created: %s", rest);

        //todo JVM just dies here
        this.entityCache = new FileCache<>(FastJSONLib.fastJsonLib, Entity.Bind.Name, CACHE_FILE, 250, this);
        logger.at(Level.INFO).log("EntityCache created: %s", entityCache);

        this.server = new RestServer(this.rest, host, port, ServerEndpoints.values());
        logger.at(Level.INFO).log("Server Started! %s", server);
    }

    public static void main(String[] args) throws IOException {
        logger.at(Level.INFO).log("Starting comroid Status Server...");
        instance = new StatusServer(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), PORT);
        logger.at(Level.INFO).log("Status Server running! Booting Discord Bot...");
        DiscordBot.INSTANCE.supplyToken(instance, args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(instance::shutdown));
        logger.at(Level.INFO).log("Shutdown Hook registered!");
    }

    public final Optional<Service> getServiceByName(String name) {
        logger.at(Level.INFO).log("Returning Service by name: %s", name);
        return entityCache.stream()
                .filter(ref -> !ref.isNull())
                .filter(ref -> ref.process().test(Service.class::isInstance))
                .map(ref -> ref.into(Service.class::cast))
                .filter(service -> service.getName().equals(name))
                .findFirst();
    }

    private void shutdown() {
        entityCache.disposeThrow();
    }
}
