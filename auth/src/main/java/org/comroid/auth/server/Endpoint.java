package org.comroid.auth.server;

import com.sun.net.httpserver.Headers;
import org.comroid.api.Polyfill;
import org.comroid.auth.user.UserAccount;
import org.comroid.auth.user.UserSession;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.node.UniNode;
import org.intellij.lang.annotations.Language;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.regex.Pattern;

import static org.comroid.auth.user.UserAccount.EMAIL;
import static org.comroid.auth.user.UserAccount.PASSWORD;
import static org.comroid.restless.HTTPStatusCodes.*;

public enum Endpoint implements ServerEndpoint.This {
    HOME("") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("login"), false);
        }
    },
    WIDGET("widget") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("widget.html"));
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    },
    API("api.js") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                return new REST.Response(OK, "application/javascript", AuthServer.WEB.createSubFile("api.js"));
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    },
    ACCOUNT("account") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("account.html"));
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    },
    REGISTRATION("register") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("register.html"));
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public REST.Response executePOST(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                String email = body.use(EMAIL).map(UniNode::asString).requireNonNull("No Email provided");
                String password = body.use(PASSWORD).map(UniNode::asString).requireNonNull("No Password provided");

                UserAccount account = AuthServer.instance.getUserManager().createAccount(email, password);

                return new REST.Response(OK, account);
            } catch (Throwable t) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not create user account", t);
            }
        }
    },
    LOGIN("login") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("login.html"));
            } catch (FileNotFoundException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public REST.Response executePOST(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                String email = body.use(EMAIL).map(UniNode::asString).requireNonNull("No Email provided");
                String password = body.use(PASSWORD).map(UniNode::asString).requireNonNull("No Password provided");

                UserSession session = AuthServer.instance.getUserManager().loginUser(email, password);

                REST.Header.List resp = new REST.Header.List();
                resp.add("Set-Cookie", String.format("org.comroid.auth=%s", session.getCookie()));
                String referrer = headers.getFirst(CommonHeaderNames.REFERER);
                referrer = referrer == null ? "" : referrer.substring(referrer.lastIndexOf('/') + 1);
                boolean isWidget = referrer.equals("widget");
                resp.add(CommonHeaderNames.REDIRECT_TARGET, isWidget ? "widget" : "account");
                return new REST.Response(MOVED_PERMANENTLY, resp);
            } catch (Throwable t) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not log in", t);
            }
        }

        @Override
        public REST.Response executeDELETE(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("logout"));
        }
    },
    LOGOUT("logout") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UserSession session = UserSession.findSession(headers);
            AuthServer.instance.getUserManager().closeSession(session);
            REST.Header.List response = new REST.Header.List();
            response.add("Set-Cookie", String.format("%s=null", UserSession.COOKIE_PREFIX));
            response.add(CommonHeaderNames.REDIRECT_TARGET, "login");
            return new REST.Response(MOVED_PERMANENTLY, response);
        }
    },
    SESSION_DATA("session") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                UserSession session = UserSession.findSession(headers);

                String accept = headers.getFirst(CommonHeaderNames.ACCEPTED_CONTENT_TYPE);
                if (accept != null && accept.equals("application/json"))
                    return new REST.Response(OK, session.getSessionData());

                String dataWrapper = String.format("const sessionData = JSON.parse('%s');", session.getSessionData().toSerializedString());
                return new REST.Response(OK, "application/javascript", new StringReader(dataWrapper));
            } catch (RestEndpointException ignored) {
                String dataWrapper = "const sessionData = undefined;";
                return new REST.Response(OK, "application/javascript", new StringReader(dataWrapper));
            }
        }
    };

    private final String extension;
    private final String[] regex;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AuthServer.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regex;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    Endpoint(String extension, @Language("RegExp") String... regex) {
        this.extension = extension;
        this.regex = regex;
        this.pattern = buildUrlPattern();
    }
}
