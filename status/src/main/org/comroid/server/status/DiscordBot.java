package org.comroid.server.status;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import org.comroid.javacord.util.commands.Command;
import org.comroid.javacord.util.commands.CommandGroup;
import org.comroid.javacord.util.commands.CommandHandler;
import org.comroid.javacord.util.ui.embed.DefaultEmbedFactory;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;

public enum DiscordBot {
    INSTANCE;

    private final CompletableFuture<String>     tokenFuture     = new CompletableFuture<>();
    private final CompletableFuture<DiscordApi> apiFuture
                                                                =
            tokenFuture.thenComposeAsync(token -> new DiscordApiBuilder().setToken(
            token)
            .setWaitForServersOnStartup(true)
            .setTotalShards(1)
            .login());
    private final CompletableFuture<Container>  containerFuture = apiFuture.thenApplyAsync(Container::new);

    public synchronized CompletableFuture<?> supplyToken(String token) {
        if (tokenFuture.isDone()) {
            throw new IllegalStateException("Duplicate Token supplied");
        }

        tokenFuture.complete(token);

        return containerFuture;
    }

    public CompletableFuture<DiscordApi> api() {
        return apiFuture;
    }

    private class Container {
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
            cmd.registerCommandTarget(Commands.INSTANCE);
        }
    }

    @CommandGroup(name = "Status Commands", description = "All commands related to the status server")
    private enum Commands {
        INSTANCE;

        @Command
        public void status() {
            // todo
        }
    }
}
