package org.comroid.status.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.crystalshard.AbstractDiscordBot;
import org.comroid.crystalshard.DiscordAPI;
import org.comroid.crystalshard.entity.channel.TextChannel;
import org.comroid.crystalshard.gateway.GatewayIntent;
import org.comroid.mutatio.ref.Reference;

import java.io.IOException;

public final class DiscordBot extends AbstractDiscordBot {
    private static final Logger logger = LogManager.getLogger();
    public static final DiscordAPI DISCORD_API;
    public static final Reference<String> token = Reference.create();
    public static final Reference<DiscordBot> instance = Reference.create();

    static {
        DiscordAPI.SERIALIZATION = StatusServer.ADAPTER_DEFINITION.serialization;
        DISCORD_API = new DiscordAPI(StatusServer.ADAPTER_DEFINITION.http);
        token.onChange(newToken -> {
            logger.info("New token found! Restarting Discord Bot...");

            try {
                if (instance.isNonNull()) {
                    instance.assertion().close();
                    instance.unset();
                }
            } catch (IOException e) {
                logger.warn("An error occurred closing the old Discord Bot", e);
                throw new RuntimeException("Error occurred restarting Discord Bot", e);
            } finally {
                DiscordBot bot = token.into(DiscordBot::new);
                instance.set(bot);
                logger.info("New Bot started");
            }
        });
    }

    protected DiscordBot(String token, GatewayIntent... gatewayIntents) {
        super(DISCORD_API, token, gatewayIntents);

        getShards().assertion()
                .readyTasks
                .add(shard -> getSnowflakeCache()
                        .getChannel(736946464118276178L)
                        .flatMap(TextChannel.class)
                        .peek(tc -> logger.info("Channel found: " + tc))
                        .into(tc -> tc.sendText("Hello World"))
                        .thenAccept(msg -> logger.info("Message sent: " + msg)));
    }
}
