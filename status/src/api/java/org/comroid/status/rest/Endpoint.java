package org.comroid.status.rest;

import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.status.DependenyObject;
import org.intellij.lang.annotations.Language;

public enum Endpoint implements AccessibleEndpoint {
    LIST_SERVICES("services"),

    SPECIFIC_SERVICE(
            "service/%s",
            "\\w[\\w\\d-]+"
    );

    private final String extension;
    private final String[] regexGroups;

    @Override
    public String getUrlBase() {
        return DependenyObject.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regexGroups;
    }

    Endpoint(String extension, @Language("RegExp") String... regexGroups) {
        this.extension = extension;
        this.regexGroups = regexGroups;
    }
}
