package org.comroid.status.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Junction;
import org.comroid.common.exception.AssertionException;
import org.comroid.common.io.FileHandle;
import org.comroid.common.java.JITAssistant;
import org.comroid.crystalshard.Context;
import org.comroid.crystalshard.DiscordAPI;
import org.comroid.crystalshard.entity.user.User;
import org.comroid.crystalshard.model.message.embed.EmbedBuilder;
import org.comroid.crystalshard.model.presence.UserStatus;
import org.comroid.crystalshard.ui.annotation.Option;
import org.comroid.crystalshard.ui.annotation.SlashCommand;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.okhttp.v4.OkHttp4Adapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.AdapterDefinition;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.server.auth.TokenCore;
import org.comroid.status.server.entity.LocalService;
import org.comroid.status.server.entity.LocalStoredService;
import org.comroid.status.server.rest.ServerEndpoints;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.FileCache;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StatusServer implements ContextualProvider.Underlying, Closeable {
    //http://localhost:42641/services

    public static final AdapterDefinition ADAPTER_DEFINITION;
    private static final Logger logger = LogManager.getLogger();
    public static final FileHandle PATH_BASE = new FileHandle("/home/comroid/srv_status/", true); // server path base
    public static final FileHandle DATA_DIR = PATH_BASE.createSubDir("data");
    public static final FileHandle BOT_TOKEN = DATA_DIR.createSubFile("discord.cred");
    public static final FileHandle ADMIN_TOKEN = DATA_DIR.createSubFile("admin.cred");
    public static final FileHandle TOKEN_DIR = PATH_BASE.createSubDir("token");
    public static final FileHandle CACHE_FILE = DATA_DIR.createSubFile("cache.json");
    public static final int PORT = 42641; // hardcoded in server, do not change
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("comroid Status Server");
    public static final DiscordAPI DISCORD;
    public static final String ADMIN_TOKEN_NAME = "admin$access$token";
    public static StatusServer instance;

    static {
        ADAPTER_DEFINITION = AdapterDefinition.initialize(FastJSONLib.fastJsonLib, new OkHttp4Adapter());
        DiscordAPI.SERIALIZATION = ADAPTER_DEFINITION.serialization;
        DISCORD = new DiscordAPI(ADAPTER_DEFINITION.http);

        logger.debug("Preparing classes...");
        JITAssistant.prepareStatic(Entity.Bind.class, Service.Bind.class);
        AssertionException.expect(3, LocalService.GROUP.streamAllChildren().count(), (x, y) -> x < y, "LocalService children count");

        if (ADMIN_TOKEN.getContent().isEmpty())
            ADMIN_TOKEN.setContent(TokenCore.generate(ADMIN_TOKEN_NAME));
    }

    public final REST rest;
    private final ScheduledExecutorService threadPool;
    private final FileCache<String, Entity> entityCache;
    private final RestServer server;
   // private final DiscordBotBase bot;
    private final Reference<UserStatus> userStatusSupplier;

    public final FileCache<String, Entity> getEntityCache() {
        return entityCache;
    }

    public final RestServer getServer() {
        return server;
    }

    public final ScheduledExecutorService getThreadPool() {
        return threadPool;
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return ADAPTER_DEFINITION;
    }

    private StatusServer(ScheduledExecutorService executor, InetAddress host, int port) throws IOException {
        try {
            instance = this;

            logger.info("Initialized Adapters");

        /*
        this.threadPool = ThreadPool.fixedSize(THREAD_GROUP, 8);
        logger.at(Level.INFO).log("ThreadPool created: %s", threadPool);
         */
            this.threadPool = executor;

            this.rest = new REST(ADAPTER_DEFINITION, threadPool);
            logger.debug("REST Client created: {}", rest);

            this.entityCache = new FileCache<>(
                    this,
                    FastJSONLib.fastJsonLib,
                    StatusServer::resolveEntity,
                    Entity.Bind.Name,
                    Junction.identity(),
                    CACHE_FILE,
                    250,
                    true
            );
            logger.debug("EntityCache created: {}", entityCache);
            entityCache.reloadData();
            logger.debug("Loaded {} services",
                    entityCache.streamRefs()
                            .filter(ref -> ref.test(Service.class::isInstance))
                            .count());

            logger.debug("Starting REST Server...");
            this.server = new RestServer(ADAPTER_DEFINITION.serialization, executor, AdapterDefinition.URL_BASE, host, port, ServerEndpoints.values());
            server.addCommonHeader("Access-Control-Allow-Origin", "*");

            getServiceByName("status-server")
                    .flatMap(LocalService.class)
                    .requireNonNull("Status server not found in cache!")
                    .discardPoll(Service.Status.ONLINE);
            getServiceByName("test-dummy")
                    .flatMap(LocalService.class)
                    .requireNonNull("Testing Dummy service not found in cache!")
                    .discardPoll(Service.Status.ONLINE);
            logger.debug("Status Server ready! {}", server);

            //this.bot = new DiscordBotBase(DISCORD, BOT_TOKEN.getContent(true));
            this.userStatusSupplier = StatusServer.instance.getEntityCache()
                    .pipe()
                    .filter(Service.class::isInstance)
                    .map(Service.class::cast)
                    .map(Service::getStatus)
                    .filter(status -> status != Service.Status.UNKNOWN)
                    .sorted(Comparator.comparingInt(Service.Status::getValue))
                    .findAny()
                    .or(() -> Service.Status.OFFLINE)
                    .map(status -> {
                        switch (status) {
                            case UNKNOWN:
                            case OFFLINE:
                            case CRASHED:
                                return UserStatus.DO_NOT_DISTURB;
                            case MAINTENANCE:
                            case NOT_RESPONDING:
                                return UserStatus.IDLE;
                            case ONLINE:
                                return UserStatus.ONLINE;
                        }
                        throw new AssertionError();
                    });
            getThreadPool().scheduleAtFixedRate(() -> {
                final UserStatus useStatus = userStatusSupplier.orElse(UserStatus.DO_NOT_DISTURB);

                String str = "";
                switch (useStatus) {
                    case ONLINE:
                        str = "All services operating normally";
                        break;
                    case IDLE:
                        str = "Some services have problems";
                        break;
                    case DO_NOT_DISTURB:
                        str = "Some services are offline";
                        break;
                    case INVISIBLE:
                    case OFFLINE:
                        throw new UnsupportedOperationException("Cannot set Bot Status",
                                new AssertionError("Invalid useStatus found"));
                }

                logger.debug("Updating presence to: {} - {}", useStatus, str);
               // bot.updatePresence(useStatus, str);
            }, 5, 30, TimeUnit.SECONDS);
            /*
            final InteractionCore core = bot.getInteractionCore();
            final CommandSetup commands = core.getCommands();
            commands.readClass(Commands.class);
            core.synchronizeGlobal().join();
             */
        } catch (Throwable t) {
            logger.error("An error occurred during startup, stopping", t);
            System.exit(0);
            throw new Error(t);
        }
    }

    private static <V extends DataContainer<V>> V resolveEntity(ContextualProvider context, UniObjectNode data) {
        return (V) new LocalStoredService(context, data);
    }

    public static void main(String... args) throws IOException {
        logger.info("Starting comroid Status Server...");
        new StatusServer(Executors.newScheduledThreadPool(4), InetAddress.getByAddress(new byte[]{0, 0, 0, 0}), PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(instance::close));
        instance.threadPool.scheduleAtFixedRate(() -> {
            try {
                instance.entityCache.storeData();
            } catch (IOException e) {
                logger.fatal("Could not store data", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        logger.info("Hooks registered!");
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

    public final Reference<Service> getServiceByName(String name) {
        logger.debug("Returning Service by name: {}", name);
        return Reference.provided(() -> entityCache.streamRefs()
                .filter(ref -> !ref.isNull())
                .filter(ref -> ref.process().test(Service.class::isInstance))
                .map(ref -> ref.into(Service.class::cast))
                .filter(service -> service.getName().equals(name))
                .findFirst()
                .orElse(null));
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

    public static final class Commands {
        @SlashCommand(name = "service", description = "Modify or view Service information")
        public static final class ServiceBlob {
            @SlashCommand(description = "Access a Service's Status")
            public static String status(
                    @Option(name = "service", description = "Name of the Service", required = true) String name,
                    @Option(name = "status", description = "The new Status") Service.Status status
            ) {
                final LocalService service = instance.entityCache.get(name).as(LocalService.class, "Invalid Service");

                final Service.Status old = service.getStatus();
                if (status == null)
                    return String.format("Status of Service `%s` is `%s`", service, old);
                service.setStatus(status);
                return String.format("Status of %s was updated from `%s` to `%s`", service.getDisplayName(), old, status);
            }

            @SlashCommand(description = "Regenerates the API Token of the Service")
            public static Void regenerate(
                    @Option(name = "service", description = "Name of the Service", required = true) String name,
                    User sender
            ) {
                final LocalService service = instance.entityCache.get(name).as(LocalService.class, "Invalid Service");

                sender.composeEmbed()
                        .setTitle(String.format("Token of %#s changed", service))
                        .setDescription(String.format("New Token: ```%s```", service.regenerateToken()))
                        .compose()
                        .join();
                return null;
            }

            @SlashCommand(description = "Lists all Services")
            public static Object list(Context context) {
                final Set<Service> services = instance.getEntityCache()
                        .streamRefs()
                        .filter(ref -> ref.test(Service.class::isInstance))
                        .map(ref -> ref.into(Service.class::cast))
                        .collect(Collectors.toSet());

                if (services.size() == 0)
                    return "No services defined!";

                final EmbedBuilder builder = new EmbedBuilder(context);

                services.forEach(service -> builder.addField(service.getDisplayName(), String.format(
                        "Service Name: `%s`\nStatus: `%s`",
                        service.getName(),
                        service.getStatus().toString()
                )));

                return builder;
            }

            @SlashCommand(description = "Creates a new Service")
            public static Object create(
                    @Option(name = "name", required = true) String serviceName,
                    @Option(name = "display_name", required = true) String displayName,
                    Context context
            ) {
                final UniObjectNode data = context.getSerializer().createObjectNode();
                data.put(Service.Bind.DisplayName, displayName);
                final Service service = StatusServer.instance.createService(serviceName, data);

                return String.format("Created new Service: %s '%s'", service.getDisplayName(), service.getName());
            }
        }
    }
}
