package org.comroid.status.server;

import org.comroid.crystalshard.AbstractDiscordBot;
import org.comroid.crystalshard.DiscordAPI;
import org.comroid.mutatio.ref.Reference;
import org.comroid.status.DependenyObject;

import java.io.IOException;

public final class DiscordBot extends AbstractDiscordBot {
    public static final DiscordAPI DISCORD_API;
    public static final Reference<String> token = Reference.create();
    public static final Reference<DiscordBot> instance = Reference.create();

    static {
        DiscordAPI.SERIALIZATION = DependenyObject.Adapters.SERIALIZATION_ADAPTER;
        DISCORD_API = new DiscordAPI(DependenyObject.Adapters.HTTP_ADAPTER);
        token.onChange(newToken -> {
            try {
                if (instance.isNonNull()) {
                    instance.assertion().close();
                    instance.unset();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error occurred restarting Discord Bot", e);
            } finally {
                DiscordBot bot = token.into(DiscordBot::new);
                instance.set(bot);
            }
        });
    }

    protected DiscordBot(String token) {
        super(DISCORD_API, token);
    }
}
