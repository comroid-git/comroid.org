package org.comroid.server.status.test.server;

import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.status.rest.Endpoint;

import java.util.regex.Pattern;

public enum LocalEndpoint implements AccessibleEndpoint {
    LIST_SERVICES(Endpoint.LIST_SERVICES),

    SPECIFIC_SERVICE(Endpoint.SPECIFIC_SERVICE),
    SERVICE_STATUS_ICON(Endpoint.SERVICE_STATUS_ICON),
    UPDATE_SERVICE_STATUS(Endpoint.UPDATE_SERVICE_STATUS),

    POLL(Endpoint.POLL);

    private final Endpoint base;

    @Override
    public String getUrlBase() {
        return StatusServerTests.LOCAL_BASE_URL;
    }

    @Override
    public String getUrlExtension() {
        return base.getUrlExtension();
    }

    @Override
    public String[] getRegExpGroups() {
        return base.getRegExpGroups();
    }

    @Override
    public Pattern getPattern() {
        return base.getPattern();
    }

    LocalEndpoint(Endpoint base) {
        this.base = base;
    }
}
