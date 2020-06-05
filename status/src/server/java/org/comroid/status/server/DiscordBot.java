package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.common.ref.Reference;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.commands.CommandHandler;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.entity.Service.Status;
import org.comroid.status.server.entity.LocalService;
import org.comroid.status.server.util.StatusContainer;
import org.comroid.uniform.cache.Cache;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public enum DiscordBot {
    INSTANCE;

    public static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public final CompletableFuture<StatusServer> serverFuture = new CompletableFuture<>();
    private final CompletableFuture<String> tokenFuture = new CompletableFuture<>();
    private final CompletableFuture<DiscordApi> apiFuture
            = tokenFuture.thenComposeAsync(token -> new DiscordApiBuilder().setToken(
            token)
            .setWaitForServersOnStartup(true)
            .setTotalShards(1)
            .login());
    private final CompletableFuture<Container> containerFuture = apiFuture.thenApplyAsync(Container::new);

    public synchronized CompletableFuture<?> supplyToken(StatusServer server, String token) {
        if (tokenFuture.isDone()) {
            throw new IllegalStateException("Duplicate Token supplied");
        }

        tokenFuture.complete(token);

        return containerFuture;
    }

    public CompletableFuture<DiscordApi> api() {
        return apiFuture;
    }

    private StatusServer server() {
        try {
            return serverFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private class Container {
        private final DiscordBot.Commands COMMANDS = new DiscordBot.Commands();
        private final DiscordApi api;
        private final CommandHandler cmd;

        private Container(DiscordApi api) {
            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(new Color(0xcf2f2f))
                    .setFooter(
                            "comroid Status Update Bot",
                            api.getYourself()
                                    .getAvatar()
                    ));

            this.api = api;
            this.cmd = new CommandHandler(api);

            cmd.prefixes = new String[]{"status!"};
            cmd.autoDeleteResponseOnCommandDeletion = true;
            cmd.useBotMentionAsPrefix = true;
            cmd.useDefaultHelp(DefaultEmbedFactory.INSTANCE);
            cmd.withUnknownCommandResponseStatus(true);
            cmd.registerCommandTarget(COMMANDS);

            api.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(() -> {
                        final UserStatus useStatus = StatusServer.instance.getEntityCache()
                                .stream()
                                .map(Reference::process)
                                .filter(ref -> ref.test(Service.class::isInstance))
                                .map(   ref -> ref.map(Service.class::cast))
                                .map(   ref -> ref.map(Service::getStatus))
                                .map(   ref -> ref.filter(status -> status != Status.UNKNOWN))
                                .sorted(Comparator.comparingInt(
                                        ref -> ref.into(Status::getValue)))
                                .map(   ref -> ref.orElse(Status.OFFLINE))
                                .map(   status -> {
                                    switch (status) {
                                        case UNKNOWN:
                                        case OFFLINE:
                                            return UserStatus.DO_NOT_DISTURB;
                                        case MAINTENANCE:
                                        case BUSY:
                                            return UserStatus.IDLE;
                                        case ONLINE:
                                            return UserStatus.ONLINE;
                                    }
                                    throw new AssertionError();
                                })
                                .findFirst()
                                .orElse(UserStatus.ONLINE);

                        String str = "";
                        switch (useStatus) {
                            case ONLINE:
                                str = "All services operating normally";
                                break;
                            case IDLE:
                                str = "Some systems have problems";
                                break;
                            case DO_NOT_DISTURB:
                                str = "Some systems are offline";
                                break;
                            case INVISIBLE:
                            case OFFLINE:
                                throw new UnsupportedOperationException("Cannot set Bot Status",
                                        new AssertionError("Invalid useStatus found"));
                        }

                        api.updateStatus(useStatus);
                        api.updateActivity(str);
                    }, 10, 30, TimeUnit.SECONDS);
        }

    }

    @CommandGroup(
            name = "Status Commands",
            description = "All commands related to the status server"
    )
    public class Commands {
        @Command(aliases = "store-data", shownInHelpCommand = false)
        public void storeData(User user) {
            if (user.getId() == 141476933849448448L) {
                try {
                    StatusServer.instance.getEntityCache().storeData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Command(
                usage = "get <str: service_name>",
                minimumArguments = 1,
                maximumArguments = 1,
                convertStringResultsToEmbed = true
        )
        public String get(String[] args, User user) {
            logger.at(Level.INFO).log("User %s requested service: %s", user, args[0]);

            return server().getServiceByName(args[0])
                    .map(service -> String.format("Service %s is currently `%s`", service.getDisplayName(), service.getStatus().toString()))
                    .orElse(String.format("No service with the name `%s` could be found", args[0]));
        }

        @Command(
                usage = "update <str: service_name> <int: new_status>",
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 2,
                maximumArguments = 2,
                convertStringResultsToEmbed = true
        )
        public String update(String[] args, User user) {
            final Status status = Status.valueOf(Integer.parseInt(args[1]));
            logger.at(Level.INFO).log("User %s update service status: %s -> %s", user, args[0], status);

            return server().getServiceByName(args[0])
                    .map(StatusContainer.class::cast)
                    .map(service -> {
                        service.setStatus(status);
                        return String.format(
                                "Updated status of Service %s to `%s`",
                                args[0],
                                service.getStatus().toString()
                        );
                    })
                    .orElse(String.format("No service with the name `%s` could be found", args[0]));
        }

        @Command(
                aliases = "list-services",
                usage = "list-services",
                maximumArguments = 0,
                convertStringResultsToEmbed = true
        )
        public Object listServices(User user) {
            logger.at(Level.INFO).log("User %s requested all services", user);

            try {
                final Set<Service> services = server().getEntityCache()
                        .stream()
                        .filter(ref -> ref.test(Service.class::isInstance))
                        .map(ref -> ref.into(Service.class::cast))
                        .collect(Collectors.toSet());

                if (services.size() == 0)
                    return "No services defined!";

                final EmbedBuilder builder = DefaultEmbedFactory.create();

                services.forEach(service -> builder.addField(service.getDisplayName(), String.format(
                        "Service Name: `%s`\nStatus: `%s`",
                        service.getName(),
                        service.getStatus().toString()
                )));

                return builder;
            } catch (Throwable any) {
                any.printStackTrace();
            }
            return null;
        }

        @Command(
                aliases = "create-service",
                usage = "create-service <str: service_name> [str: display_name]",
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 1,
                maximumArguments = 2,
                convertStringResultsToEmbed = true
        )
        public String createService(String[] args, User user) {
            logger.at(Level.INFO).log("User %s is creating service: %s", user, args[0]);

            final Service service = new LocalService.Builder().with(Service.Bind.Name, args[0])
                    .with(Service.Bind.DisplayName, args.length >= 2 ? args[1] : args[0])
                    .build();

            final Cache<String, Entity> entityCache = server().getEntityCache();
            entityCache.getReference(args[0], true).set(service);

            return String.format("Created new Service: %s '%s'", service.getName(), service.getDisplayName());
        }
    }
}
