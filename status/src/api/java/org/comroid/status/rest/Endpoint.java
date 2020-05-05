package org.comroid.status.rest;

import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

import org.comroid.common.iter.Operator;
import org.comroid.restless.RestEndpoint;
import org.comroid.status.DependenyObject;

import org.intellij.lang.annotations.Language;

public enum Endpoint implements RestEndpoint {
    LIST_SERVICES("services"),
    GET_SERVICE("sevices/%s",
            "services/(\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b)",
            Operator.intOrder(0)
    );

    private final String           extension;
    private final Pattern          urlPattern;
    private final IntUnaryOperator groupFx;

    Endpoint(String extension) {
        //noinspection LanguageMismatch
        this(extension, extension, x -> x);
    }

    Endpoint(String extension, @Language("RegExp") String regex, IntUnaryOperator groupFx) {
        this.extension  = extension;
        this.urlPattern = Pattern.compile(DependenyObject.URL_BASE + regex);
        this.groupFx    = groupFx;
    }

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

    @Override
    public IntUnaryOperator getGroupFx() {
        return groupFx;
    }
}
