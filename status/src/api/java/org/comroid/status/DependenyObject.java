package org.comroid.status;

import org.comroid.api.Polyfill;
import org.comroid.listnr.EventManager;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.entity.Service;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.uniform.SerializationAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public interface DependenyObject extends EventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload> {
    String URL_BASE = "https://api.status.comroid.org/";
    URI GATEWAY_URI = Polyfill.uri("wss://gateway.status.comroid.org");

    static Service resolveService(DependenyObject dependenyObject, String name) {
        return null; //todo
    }

    final class Adapters {
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull SerializationAdapter<?, ?, ?> SERIALIZATION_ADAPTER = null;
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull HttpAdapter HTTP_ADAPTER = null;
    }
}
