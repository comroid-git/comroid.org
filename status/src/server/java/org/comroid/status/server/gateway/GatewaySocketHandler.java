package org.comroid.status.server.gateway;

import org.comroid.restless.socket.WebSocketServer;
import org.comroid.restless.socket.event.WebSocketEvent;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.DependenyObject;
import org.comroid.status.DependenyObject.Adapters;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.net.Socket;

public class GatewaySocketHandler extends WebSocketServer.SocketHandler {
    public GatewaySocketHandler(WebSocketServer server, Socket socket) {
        super(server, socket);

        getInitialHeaders()
                .thenApply(headers -> {
                    final UniObjectNode hello = Adapters.SERIALIZATION_ADAPTER.createUniObjectNode();
                    hello.put("Authentication", ValueType.STRING, "tobu");

                    enqueue(hello.toString());

                    return hello;
                });
    }
}
