package org.comroid.status.rest;

import org.comroid.restless.endpoint.RestEndpoint;
import org.comroid.status.DependenyObject;
import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum Endpoint implements RestEndpoint {
    LIST_SERVICES("services");

    private final String extension;
    private final Pattern urlPattern;

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String getUrlBase() {
        return DependenyObject.URL_BASE;
    }

    @Override
    public Pattern getPattern() {
        return urlPattern;
    }

    Endpoint(String extension) {
        //noinspection LanguageMismatch
        this(extension, extension);
    }

    Endpoint(String extension, @Language("RegExp") String regex) {
        this.extension = extension;
        this.urlPattern = Pattern.compile(DependenyObject.URL_BASE + regex);
    }
}
