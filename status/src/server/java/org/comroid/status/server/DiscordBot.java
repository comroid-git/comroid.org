package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.dux.DiscordUX;
import org.comroid.dux.form.DiscordForm;
import org.comroid.dux.javacord.JavacordDUX;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandHandler;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;
import org.comroid.mutatio.ref.Reference;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Service;
import org.comroid.status.entity.Service.Status;
import org.comroid.status.server.entity.LocalService;
import org.comroid.uniform.node.UniObjectNode;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private final CompletableFuture<Container> containerFuture = apiFuture.thenApplyAsync(Container::new)
            .exceptionally(ExceptionLogger.get());

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

    private static final class ModifyCommandUtil {
        private enum MainMenuSelection implements Named {
            CHANGE_STATUS("status", "\uD83D\uDEA6", "Update Status");

            private final String target;
            private final String emoji;
            private final String name;

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getAlternateFormattedName() {
                return emoji;
            }

            public String getOptionName() {
                return target;
            }

            MainMenuSelection(String targetOption, String emoji, String name) {
                this.target = targetOption;
                this.emoji = emoji;
                this.name = name;
            }
        }

        private enum StatusSelection implements Named {
            OFFLINE(Status.OFFLINE, "\uD83D\uDEA8", "Set to Offline"),
            MAINTENANCE(Status.MAINTENANCE, "\uD83D\uDEA7", "Set to Maintenance"),
            ONLINE(Status.ONLINE, "\uD83C\uDF4F", "Set to Online"),

            GO_BACK(Status.UNKNOWN, "⬅️", "Go back to the menu");

            private final String emoji;
            private final String name;
            private final Status status;

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getAlternateFormattedName() {
                return emoji;
            }

            StatusSelection(Status status, String emoji, String name) {
                this.emoji = emoji;
                this.name = name;
                this.status = status;
            }
        }
    }

    private class Container {
        private final DiscordBot.Commands COMMANDS = new DiscordBot.Commands(this);
        private final DiscordApi api;
        private final CommandHandler cmd;
        private final DiscordUX<Server, TextChannel, User, Message> dux;
        private final Reference<UserStatus> userStatusSupplier = StatusServer.instance.getEntityCache()
                .pipe()
                .filter(Service.class::isInstance)
                .map(Service.class::cast)
                .map(Service::getStatus)
                .filter(status -> status != Status.UNKNOWN)
                .sorted(Comparator.comparingInt(Status::getValue))
                .findAny()
                .or(() -> Status.OFFLINE)
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

        private Container(DiscordApi api) {
            DefaultEmbedFactory.setEmbedSupplier(() -> new EmbedBuilder().setColor(new Color(0xcf2f2f))
                    .setFooter(
                            "comroid Status Update Bot",
                            api.getYourself().getAvatar()
                    ));

            this.api = api;
            this.cmd = new CommandHandler(api);
            this.dux = DiscordUX.create(new JavacordDUX(api));

            cmd.prefixes = new String[]{"status!"};
            cmd.autoDeleteResponseOnCommandDeletion = true;
            cmd.useBotMentionAsPrefix = true;
            cmd.useDefaultHelp(DefaultEmbedFactory.INSTANCE);
            cmd.withUnknownCommandResponseStatus(true);
            cmd.registerCommandTarget(COMMANDS);

            api.getThreadPool()
                    .getScheduler()
                    .scheduleAtFixedRate(() -> {
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

                        api.updateStatus(useStatus);
                        api.updateActivity(str);
                    }, 5, 30, TimeUnit.SECONDS);
        }

    }

    public class Commands {
        private final Container container;
        private final Map<Service, DiscordForm<Server, TextChannel, User, Message>> serviceForms = new ConcurrentHashMap<>();

        private Commands(Container container) {
            this.container = container;
        }

        @Command(
                aliases = "store-data",
                enablePrivateChat = false,
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR
        )
        public void storeData(User user) throws IOException {
            StatusServer.instance.getEntityCache().storeData();
        }

        @Command(
                usage = "modify <str: service_name>",
                enablePrivateChat = false,
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 1,
                maximumArguments = 1,
                async = true
        )
        public void modify(String[] args, TextChannel channel, User user) {
            final LocalService service = StatusServer.instance.getServiceByName(args[0]).into(LocalService.class::cast);
            final DiscordUX<Server, TextChannel, User, Message> dux = container.dux;

            getFormForService(dux, service, user).execute(channel, user);
        }

        private DiscordForm<Server, TextChannel, User, Message> getFormForService(DiscordUX<Server, TextChannel, User, Message> dux, LocalService service, User user) {
            return serviceForms.computeIfAbsent(service, k -> dux.createForm()
                    .addEnumSelection("menu", ModifyCommandUtil.MainMenuSelection.class,
                            DefaultEmbedFactory.create(user).setDescription("Select what to change"),
                            ModifyCommandUtil.MainMenuSelection::getOptionName)
                    .addEnumSelection("status", ModifyCommandUtil.StatusSelection.class,
                            DefaultEmbedFactory.create(user).setDescription("Select new Status"),
                            sel -> {
                                if (sel == ModifyCommandUtil.StatusSelection.GO_BACK)
                                    return "menu";
                                service.setStatus(sel.status);
                                return "menu";
                            }));
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
                aliases = "regen-token",
                usage = "regen-token <str: service_name>",
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 1,
                maximumArguments = 1,
                convertStringResultsToEmbed = true
        )
        public CompletableFuture<String> regenerateToken(String[] args, Server server, User user) {
            final LocalService service = server().getServiceByName(args[0])
                    .flatMapOptional(it -> it.as(LocalService.class))
                    .orElseThrow(() -> new NoSuchElementException(String.format("No Service found with name `%s`", args[0])));

            return user.sendMessage(DefaultEmbedFactory.create(server, user)
                    .addField("Token Regenerated!", String.format(
                            "New Token for %s: ```%s```",
                            service,
                            service.regenerateToken()
                    )))
                    .thenApply(nil -> String.format("Token for %s regenerated!", service));
        }

        @Command(
                usage = "update <str: service_name> <status: new_status>",
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 2,
                maximumArguments = 2,
                convertStringResultsToEmbed = true
        )
        public String update(String[] args, User user) {
            final Status status = args[1].matches("\\d+")
                    ? Status.valueOf(Integer.parseInt(args[1]))
                    : Status.valueOf(args[1].toUpperCase());
            logger.at(Level.INFO).log("User %s update service status: %s -> %s", user, args[0], status);

            return server().getServiceByName(args[0])
                    .flatMapOptional(service -> service.as(LocalService.class))
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
                        .streamRefs()
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
            final String serviceName = args[0];
            logger.at(Level.INFO).log("User %s is creating service: %s", user, serviceName);

            final UniObjectNode data = DependenyObject.Adapters.SERIALIZATION_ADAPTER.createUniObjectNode();
            data.put(Service.Bind.DisplayName, args.length >= 2 ? args[1] : serviceName);
            final Service service = StatusServer.instance.createService(serviceName, data);

            return String.format("Created new Service: %s '%s'", service.getName(), service.getDisplayName());
        }

        @Command(
                aliases = "set-url",
                usage = "set-url <str: service_name> <str: new url>",
                requiredDiscordPermissions = PermissionType.ADMINISTRATOR,
                minimumArguments = 2,
                maximumArguments = 2,
                convertStringResultsToEmbed = true
        )
        public String updateUrl(String[] args) {
            return server().getServiceByName(args[0])
                    .map(service -> {
                        service.put(Polyfill.uncheckedCast(Service.Bind.URL), args[1]);

                        return String.format("Updated URL of Service %s!", service);
                    })
                    .orElseThrow(() -> new NoSuchElementException("No Service found with name " + args[0]));
        }
    }
}
