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

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.auth.user.UserAccount.EMAIL;
import static org.comroid.auth.user.UserAccount.PASSWORD;
import static org.comroid.restless.CommonHeaderNames.COOKIE;
import static org.comroid.restless.HTTPStatusCodes.*;

public enum Endpoint implements ServerEndpoint.This {
    HOME("") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("login"), false);
        }
    },
    API("api.js") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "application/javascript", AuthServer.WEB.createSubFile("api.js"));
        }
    },
    ACCOUNT("account") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("account.html"));
        }
    },
    REGISTRATION("register") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("register.html"));
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
            return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("login.html"));
        }

        @Override
        public REST.Response executePOST(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                String email = body.use(EMAIL).map(UniNode::asString).requireNonNull("No Email provided");
                String password = body.use(PASSWORD).map(UniNode::asString).requireNonNull("No Password provided");

                UserSession session = AuthServer.instance.getUserManager().loginUser(email, password);

                REST.Header.List resp = new REST.Header.List();
                resp.add("Set-Cookie", String.format("org.comroid.auth=%s", session.getCookie()));
                resp.add(CommonHeaderNames.REDIRECT_TARGET, "account");
                return new REST.Response(MOVED_PERMANENTLY, resp);
            } catch (Throwable t) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not log in", t);
            }
        }
    },
    SESSION_DATA("session_data") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            String[] cookies = headers.getFirst(COOKIE).split("; ");
            UserSession session = Stream.of(cookies)
                    .filter(UserSession::isAppCookie)
                    .map(str -> str.substring(UserSession.COOKIE_PREFIX.length() + 1))
                    .map(c -> {
                        try {
                            return AuthServer.instance.getUserManager().findSession(c);
                        } catch (Throwable ignored) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED));
            REST.Header.List response = new REST.Header.List();
            response.add(COOKIE, session.getCookie());
            return new REST.Response(OK, session.getSessionData(), response);
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
