package org.comroid.auth.rest;

import org.comroid.api.Polyfill;
import org.comroid.auth.AuthServer;
import org.comroid.auth.user.User;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum AuthEndpoint implements AccessibleEndpoint {
    MODIFY_ACCOUNT("/account/%s", Polyfill.UUID_PATTERN),
    MODIFY_ACCOUNT_DATA_STORAGE("/account/%s/service/%s/data/%s", Polyfill.UUID_PATTERN, Polyfill.UUID_PATTERN, User.STORAGE_NAME_PATTERN),
    SERVICES("/api/services"),
    SERVICE_API("/api/service/%s", Polyfill.UUID_PATTERN);

    private final String extension;
    private final String[] regExp;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AuthServer.URL_BASE;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    AuthEndpoint(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
        this.pattern = buildUrlPattern();
    }
}
