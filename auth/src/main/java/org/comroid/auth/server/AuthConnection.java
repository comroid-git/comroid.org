package org.comroid.auth.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.UUIDContainer;
import org.comroid.auth.service.ServiceManager;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserManager;
import org.comroid.auth.user.UserSession;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public final class AuthConnection extends WebkitConnection {
    private static final Logger logger = LogManager.getLogger();
    private final UserSession session;

    public boolean isLoggedIn() {
        return session != null;
    }

    public Optional<UserSession> getSession() {
        return Optional.ofNullable(session);
    }

    public AuthConnection(WebSocket socketBase, REST.Header.List headers, ContextualProvider context) {
        super(socketBase, headers, context);
        UserSession session = null;
        try {
            session = UserSession.findSession(headers);
            session.connection.set(this);

        } catch (RestEndpointException unauthorized) {
            session = null;
        } finally {
            this.session = session;
        }
        setProperty("isValidSession", session != null);

        if (session != null) {
            // set session data reference
            UniObjectNode sessionData = this.session.getSessionData();
            setProperty("sessionData", sessionData);
            /*
            properties.getReference("sessionData", true)
                    .rebind(this.session::getSessionData);
             */

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

    @Override
    protected void handleCommand(
            Map<String, Object> pageProperties,
            String commandCategory,
            String commandName,
            UniNode data,
            UniObjectNode response
    ) {
        if (!isLoggedIn() || !session.getAccount().getPermits().contains(Permit.ADMIN))
            return;

        switch (commandCategory) {
            case "admin":
                switch (commandName) {
                    case "listUsers":
                        UniArrayNode users = response.putArray("users");
                        requireFromContext(UserManager.class)
                                .getUsers()
                                .map(UUIDContainer::getUUID)
                                .map(UUID::toString)
                                .forEach(id -> users.add(StandardValueType.STRING, id));
                        break;
                    case "listServices":
                        UniArrayNode services = response.putArray("services");
                        requireFromContext(ServiceManager.class)
                                .getServices()
                                .map(UUIDContainer::getUUID)
                                .map(UUID::toString)
                                .forEach(id -> services.add(StandardValueType.STRING, id));
                        break;
                    default:
                        throw new NoSuchElementException("Unknown Admin Command: " + commandName);
                }
                break;
            default:
                throw new NoSuchElementException("Unknown Command: " + commandCategory + "/" + commandName);
        }
    }
}
