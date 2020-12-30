package org.comroid.status.server;

import com.google.common.flogger.FluentLogger;
import org.comroid.crystalshard.AbstractDiscordBot;
import org.comroid.crystalshard.DiscordAPI;
import org.comroid.mutatio.ref.Reference;

import java.io.IOException;
import java.util.logging.Level;

public final class DiscordBot extends AbstractDiscordBot {
    public static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    public static final DiscordAPI DISCORD_API;
    public static final Reference<String> token = Reference.create();
    public static final Reference<DiscordBot> instance = Reference.create();

    static {
        DiscordAPI.SERIALIZATION = StatusServer.ADAPTER_DEFINITION.serialization;
        DISCORD_API = new DiscordAPI(StatusServer.ADAPTER_DEFINITION.http);
        token.onChange(newToken -> {
            LOGGER.atInfo().log("New token found! Restarting Discord Bot...");

            try {
                if (instance.isNonNull()) {
                    instance.assertion().close();
                    instance.unset();
                }
            } catch (IOException e) {
                LOGGER.at(Level.SEVERE)
                        .withCause(e)
                        .log("An error occurred closing the old Discord Bot");
                throw new RuntimeException("Error occurred restarting Discord Bot", e);
            } finally {
                DiscordBot bot = token.into(DiscordBot::new);
                instance.set(bot);
                LOGGER.atInfo().log("New Bot started");
            }
        });
    }

    protected DiscordBot(String token) {
        super(DISCORD_API, token);
    }
}
