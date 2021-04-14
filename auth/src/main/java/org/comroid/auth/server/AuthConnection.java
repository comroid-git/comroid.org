package org.comroid.auth.server;

import org.comroid.restless.REST;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;

import java.util.concurrent.Executor;

public final class AuthConnection extends WebkitConnection {
    public AuthConnection(WebSocket socketBase, REST.Header.List headers, Executor executor) {
        super(socketBase, headers, executor);
    }
}
