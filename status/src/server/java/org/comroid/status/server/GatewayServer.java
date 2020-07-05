package org.comroid.status.server;

import org.comroid.listnr.AbstractEventManager;
import org.comroid.restless.socket.WebSocketServer;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.status.server.gateway.GatewaySocketHandler;
import org.comroid.uniform.SerializationAdapter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;

public class GatewayServer extends AbstractEventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload> {
    private final WebSocketServer server;

    public GatewayServer(
            StatusServer server,
            SerializationAdapter<?, ?, ?> seriLib,
            Executor executor,
            InetAddress adress,
            int port
    ) throws IOException {
        super(server);

        this.server = new WebSocketServer(seriLib, executor, GatewaySocketHandler::new, adress, port);
    }
}