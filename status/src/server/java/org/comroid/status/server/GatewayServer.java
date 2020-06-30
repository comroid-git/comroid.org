package org.comroid.status.server;

import org.comroid.api.UUIDContainer;
import org.comroid.listnr.EventManager;
import org.comroid.listnr.ListnrCore;
import org.comroid.restless.socket.WebSocketServer;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.uniform.SerializationAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;

public class GatewayServer extends UUIDContainer
        implements EventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload> {
    private final WebSocketServer server;

    public GatewayServer(
            SerializationAdapter<?, ?, ?> seriLib,
            Executor executor,
            InetAddress adress,
            int port
    ) throws IOException {
        this.server = new WebSocketServer(seriLib, executor, it -> it.enqueue("todo"), adress, port);
    }

    @Override
    public ListnrCore listnr() {
        return server.listnr();
    }
}