package org.comroid.status.rest;

import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.status.AdapterDefinition;
import org.comroid.status.entity.Service;
import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

public enum Endpoint implements AccessibleEndpoint {
    LIST_SERVICES("services"),

    SPECIFIC_SERVICE(
            "service/%s",
            Service.NAME_REGEX
    ),
    SERVICE_STATUS_ICON(
            "service/%s/statusicon",
            Service.NAME_REGEX
    ),
    UPDATE_SERVICE_STATUS(
            "service/%s/status",
            Service.NAME_REGEX
    ),

    POLL(
            "service/%s/poll",
            Service.NAME_REGEX
    ),
    ADMIN_AUTHORIZE("admin");

    private final String extension;
    private final String[] regexGroups;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AdapterDefinition.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regexGroups;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    Endpoint(String extension, @Language("RegExp") String... regexGroups) {
        this.extension = extension;
        this.regexGroups = regexGroups;
        this.pattern = buildUrlPattern();
    }
}
