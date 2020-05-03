package org.comroid.status.rest;

import org.comroid.restless.RestEndpoint;
import org.comroid.status.StatusServer;

public enum Endpoint implements RestEndpoint {
    LIST_SERVICES("services");

    private final String extension;

    Endpoint(String extension) {
        this.extension = extension;
    }

    @Override
    public String getUrlBase() {
        return StatusServer.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }
}
