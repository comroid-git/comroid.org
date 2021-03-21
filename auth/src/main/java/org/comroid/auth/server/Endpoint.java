package org.comroid.auth.server;

import com.sun.net.httpserver.Headers;
import org.comroid.api.Polyfill;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.node.UniNode;
import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

import static org.comroid.restless.HTTPStatusCodes.OK;

public enum Endpoint implements ServerEndpoint.This {
    HOME("") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("register"), false);
        }
    },
    REGISTER_PAGE("register") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("register.html"));
        }
    },
    LOGIN_PAGE("login") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "text/html", AuthServer.WEB.createSubFile("login.html"));
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
