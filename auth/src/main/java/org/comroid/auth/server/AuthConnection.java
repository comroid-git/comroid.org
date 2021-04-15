package org.comroid.auth.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.auth.user.UserSession;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.webkit.config.WebkitConfiguration;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
            properties.put("sessionData", sessionData);
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
    protected void handleCommand(UniNode command) {
        logger.trace("Incoming command: {}", command);
        String commandName = command.get("type").asString();
        String[] split = commandName.split("/");

        UniNode data = command.wrap("data").orElse(UniValueNode.NULL);
        UniObjectNode response = findSerializer().createObjectNode().asObjectNode();
        Map<String, Object> pageProperties = requireFromContext(PagePropertiesProvider.class)
                .findPageProperties(getHeaders());

        switch (split[0]) {
            case "action":
                switch (split[1]) {
                    case "changePanel":
                        String target = data.get("target").asString();
                        try (
                                InputStream is = WebkitConfiguration.get().getPanel(target);
                                InputStreamReader isr = new InputStreamReader(is);
                                BufferedReader br = new BufferedReader(isr)
                        ) {
                            String panelData = br.lines().collect(Collectors.joining("\n"));
                            Document doc = Jsoup.parse(panelData);
                            FrameBuilder.fabricateDocument(doc, host, pageProperties);

                            response.put("type", "changePanel");
                            response.put("data", doc.toString());
                            break;
                        } catch (Throwable e) {
                            logger.error("Could not read target panel " + target, e);
                        }
                    case "refresh":
                        response.put("type", "inject");
                        UniObjectNode eventData = response.putObject("data");
                        eventData.putAll(pageProperties);
                        break;
                    default:
                        logger.error("Unknown action: {}", split[1]);
                }
                break;
            default:
                logger.error("Unknown Command received: {}", commandName);
                break;
        }

        sendText(response);
    }
}
