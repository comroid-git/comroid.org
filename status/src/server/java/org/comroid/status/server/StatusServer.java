package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.api.Junction;
import org.comroid.common.io.FileHandle;
import org.comroid.listnr.AbstractEventManager;
import org.comroid.listnr.ListnrCore;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.okhttp.v4.OkHttp3Adapter;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.status.server.entity.LocalService;
import org.comroid.status.server.rest.ServerEndpoints;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.varbind.FileCache;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StatusServer
        extends AbstractEventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload>
        implements DependenyObject, Closeable {
    //http://localhost:42641/services

    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static final FileHandle PATH_BASE = new FileHandle("/home/comroid/srv_status/", true); // server path base
    public static final FileHandle DATA_DIR = PATH_BASE.createSubDir("data");
    public static final FileHandle TOKEN_DIR = PATH_BASE.createSubDir("token");
    public static final FileHandle CACHE_FILE = DATA_DIR.createSubFile("cache.json");
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final int GATEWAY_PORT = 42642; // hardcoded in server, do not change
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static StatusServer instance;

    static {
        logger.at(Level.INFO).log("Preparing classes...");

        final long count = LocalService.GROUP.streamAllChildren().count();
        if (count < 3)
            throw new IllegalStateException("Illegal children on LocalService group");
    }

    private final ScheduledExecutorService threadPool;
    private final FileCache<String, Entity, DependenyObject> entityCache;
    private final REST<StatusServer> rest;
    private final RestServer server;
    private final GatewayServer gatewayServer;

    public final FileCache<String, Entity, DependenyObject> getEntityCache() {
        return entityCache;
    }

    public final RestServer getServer() {
        return server;
    }

    public final ScheduledExecutorService getThreadPool() {
        return threadPool;
    }

    private StatusServer(ScheduledExecutorService executor, InetAddress host, int port) throws IOException {
        super(new ListnrCore(executor));

        Adapters.HTTP_ADAPTER = new OkHttp3Adapter();
        Adapters.SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;

        logger.at(Level.INFO).log("Initialized Adapters");

        /*
        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        logger.at(Level.INFO).log("ThreadPool created: %s", threadPool);
         */
        this.threadPool = executor;

        this.rest = new REST<>(DependenyObject.Adapters.HTTP_ADAPTER, DependenyObject.Adapters.SERIALIZATION_ADAPTER, threadPool, this);
        logger.at(Level.INFO).log("REST Client created: %s", rest);

        this.entityCache = new FileCache<>(
                FastJSONLib.fastJsonLib,
                Entity.Bind.Name,
                Junction.identity(),
                CACHE_FILE,
                250,
                true,
                this
        );
        logger.at(Level.INFO).log("EntityCache created: %s", entityCache);
        logger.at(Level.INFO).log("Loaded %d services",
                entityCache.stream()
                        .filter(ref -> ref.test(Service.class::isInstance))
                        .count());

        this.server = new RestServer(this.rest, DependenyObject.URL_BASE, host, port, ServerEndpoints.values());
        this.gatewayServer = new GatewayServer(this, Adapters.SERIALIZATION_ADAPTER, this.threadPool, host, GATEWAY_PORT);
        server.addCommonHeader("Access-Control-Allow-Origin", "*");
        logger.at(Level.INFO).log("Server Started! %s", server);
    }

    public static void main(String[] args) throws IOException {
        logger.at(Level.INFO).log("Starting comroid Status Server...");
        instance = new StatusServer(Executors.newScheduledThreadPool(4), InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), PORT);
        DiscordBot.INSTANCE.serverFuture.complete(instance);

        logger.at(Level.INFO).log("Status Server running! Booting Discord Bot...");
        DiscordBot.INSTANCE.supplyToken(instance, args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(instance::close));
        instance.threadPool.scheduleAtFixedRate(() -> {
            try {
                instance.entityCache.storeData();
            } catch (IOException e) {
                logger.at(Level.SEVERE)
                        .withCause(e)
                        .log("Could not store data");
            }
        }, 5, 5, TimeUnit.MINUTES);
        logger.at(Level.INFO).log("Hooks registered!");
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

    @Override
    public void close() {
        //todo: Close resources here

        try {
            entityCache.storeData();
            entityCache.disposeThrow();
        } catch (IOException e) {
            throw new RuntimeException("Could not shut down status server properly", e);
        }
    }
}
