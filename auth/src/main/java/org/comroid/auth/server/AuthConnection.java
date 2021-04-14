package org.comroid.auth.server;

import org.comroid.auth.user.UserSession;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;

import java.util.Optional;
import java.util.concurrent.Executor;

public final class AuthConnection extends WebkitConnection {
    private final UserSession session;

    public boolean isLoggedIn() {
        return session != null;
    }

    public Optional<UserSession> getSession() {
        return Optional.ofNullable(session);
    }

    public AuthConnection(WebSocket socketBase, REST.Header.List headers, Executor executor) {
        super(socketBase, headers, executor);
        UserSession session = null;
        try {
            session = UserSession.findSession(headers);
            session.connection.set(this);

            setProperty("sessionData", session.getSessionData().toSerializedString());
        } catch (RestEndpointException unauthorized) {
            session = null;
        } finally {
            this.session = session;
        }

        if (session != null) {
            // unset connection in session
            final RefContainer<WebsocketPacket.Type, WebsocketPacket> closeListener = on(WebsocketPacket.Type.CLOSE);
            closeListener.peek(close -> {
                this.session.connection.unset();
                closeListener.close();
            });

            // send session data
            UniObjectNode eventData = session.getSessionData()
                    .surroundWithObject("sessionData")
                    .surroundWithObject("data");
            eventData.put("type", "inject");
            sendText(eventData);
        }
    }
}
