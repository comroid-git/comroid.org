package org.comroid.status.server;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.comroid.common.info.MessageSupplier;
import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.commands.CommandHandler;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public enum DiscordBot {
    INSTANCE;

    private final CompletableFuture<StatusServer> serverFuture    = new CompletableFuture<>();
    private final CompletableFuture<String>       tokenFuture     = new CompletableFuture<>();
    private final CompletableFuture<DiscordApi>   apiFuture
                                                                  =
            tokenFuture.thenComposeAsync(token -> new DiscordApiBuilder().setToken(
            token)
            .setWaitForServersOnStartup(true)
            .setTotalShards(1)
            .login());
    private final CompletableFuture<Container>    containerFuture = apiFuture.thenApplyAsync(Container::new);

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
        private final Commands       COMMANDS = new Commands();
        private final DiscordApi     api;
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

            cmd.prefixes                            = new String[]{ "status!" };
            cmd.autoDeleteResponseOnCommandDeletion = true;
            cmd.useBotMentionAsPrefix               = true;
            cmd.useDefaultHelp(null);
            cmd.withUnknownCommandResponseStatus(true);
            cmd.registerCommandTarget(COMMANDS);
        }

        @CommandGroup(name = "Status Commands", description = "All commands related to the status server")
        private class Commands {
            @Command(minimumArguments = 1, convertStringResultsToEmbed = true)
            public String status(String[] args) {
                return server().getServiceByName(args[0])
                        .map(service -> String.format("Service %s is %s", service.getName(), service.getStatus()))
                        .orElseGet(MessageSupplier.format("Service with name %s not found", args[0]));
            }
        }
    }
}
