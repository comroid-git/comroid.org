package org.comroid.oauth.rest;

import org.comroid.api.StreamSupplier;
import org.comroid.auth.server.AuthServer;
import org.comroid.restless.server.ServerEndpoint;
import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum OAuthEndpoint implements ServerEndpoint.This {
    AUTHORIZE("/authorize");

    public static final StreamSupplier<ServerEndpoint> values = StreamSupplier.of(values());
    private final String extension;
    private final String[] regExp;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AuthServer.URL_BASE + "/oauth2";
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    OAuthEndpoint(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
        this.pattern = buildUrlPattern();
    }
}
