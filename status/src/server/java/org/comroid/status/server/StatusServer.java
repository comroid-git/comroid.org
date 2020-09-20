package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.api.Junction;
import org.comroid.commandline.CommandLineArgs;
import org.comroid.common.exception.AssertionException;
import org.comroid.common.io.FileHandle;
import org.comroid.common.jvm.JITAssistant;
import org.comroid.mutatio.proc.Processor;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.okhttp.v4.OkHttp3Adapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.entity.LocalService;
import org.comroid.status.server.entity.LocalStoredService;
import org.comroid.status.server.rest.ServerEndpoints;
import org.comroid.status.server.test.StatusServerTestSite;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.FileCache;
import org.comroid.varbind.container.DataContainerBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.comroid.status.DependenyObject.Adapters.SERIALIZATION_ADAPTER;

public class StatusServer implements DependenyObject, Closeable {
    //http://localhost:42641/services

    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    public static final FileHandle PATH_BASE = new FileHandle("/home/comroid/srv_status/", true); // server path base
    public static final FileHandle DATA_DIR = PATH_BASE.createSubDir("data");
    public static final FileHandle BOT_TOKEN = DATA_DIR.createSubFile("discord.cred");
    public static final FileHandle ADMIN_TOKEN = DATA_DIR.createSubFile("admin.cred");
    public static final FileHandle TOKEN_DIR = PATH_BASE.createSubDir("token");
    public static final FileHandle CACHE_FILE = DATA_DIR.createSubFile("cache.json");
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static final String ADMIN_TOKEN_NAME = "admin$access$token";
    public static CommandLineArgs ARGS;
    public static StatusServer instance;

    static {
        logger.at(Level.INFO).log("Preparing classes...");
        JITAssistant.prepareStatic(Entity.Bind.class, Service.Bind.class);
        AssertionException.expect(3, LocalService.GROUP.streamAllChildren().count(), "LocalService children count");

        if (ADMIN_TOKEN.getContent().isEmpty())
            ADMIN_TOKEN.setContent(TokenCore.generate(ADMIN_TOKEN_NAME));
    }

    public final REST<StatusServer> rest;
    private final ScheduledExecutorService threadPool;
    private final FileCache<String, Entity, DependenyObject> entityCache;
    private final RestServer server;

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
        instance = this;

        Adapters.HTTP_ADAPTER = new OkHttp3Adapter();
        SERIALIZATION_ADAPTER = FastJSONLib.fastJsonLib;

        logger.at(Level.INFO).log("Initialized Adapters");

        /*
        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        logger.at(Level.INFO).log("ThreadPool created: %s", threadPool);
         */
        this.threadPool = executor;

        this.rest = new REST<>(
                DependenyObject.Adapters.HTTP_ADAPTER,
                SERIALIZATION_ADAPTER,
                this,
                threadPool
        );
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
                entityCache.streamRefs()
                        .filter(ref -> ref.test(Service.class::isInstance))
                        .count());

        logger.at(Level.INFO).log("Starting REST Server...");
        this.server = new RestServer(SERIALIZATION_ADAPTER, executor, DependenyObject.URL_BASE, host, port, ServerEndpoints.values());
        server.addCommonHeader("Access-Control-Allow-Origin", "*");
        logger.at(Level.INFO).log("Status Server ready! %s", server);
    }

    public static void main(String[] args) throws IOException {
        ARGS = CommandLineArgs.parse(args);
        logger.at(Level.INFO).log("Starting comroid Status Server...");
        new StatusServer(Executors.newScheduledThreadPool(4), InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), PORT);
        DiscordBot.INSTANCE.serverFuture.complete(instance);

        logger.at(Level.INFO).log("Status Server running! Booting Discord Bot...");
        DiscordBot.INSTANCE.supplyToken(instance, ARGS.wrap("token").orElseGet(BOT_TOKEN::getContent));

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

        if (ARGS.hasName("test"))
            StatusServerTestSite.start();
    }

    public LocalService createService(String serviceName, UniObjectNode data) {
        DataContainerBuilder<LocalStoredService> builder = new DataContainerBuilder<>(LocalStoredService.class, data, this);

        builder.setValue(Service.Bind.Name, serviceName);
        if (!data.has(Service.Bind.Status))
            builder.setValue(Service.Bind.Status, Service.Status.UNKNOWN.getValue());

        final LocalService service = builder.build();
        entityCache.getReference(serviceName, true).set(service);

        return service;
    }

    public final Processor<Service> getServiceByName(String name) {
        logger.at(Level.INFO).log("Returning Service by name: %s", name);
        return Processor.providedOptional(() -> entityCache.streamRefs()
                .filter(ref -> !ref.isNull())
                .filter(ref -> ref.process().test(Service.class::isInstance))
                .map(ref -> ref.into(Service.class::cast))
                .filter(service -> service.getName().equals(name))
                .findFirst());
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
