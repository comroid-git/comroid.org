package org.comroid.status.server.rest;

import org.comroid.restless.REST;
import org.comroid.restless.endpoint.RestEndpoint;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.ServerEndpoint;

public enum ServerEndpoints implements ServerEndpoint.Underlying {
    ;

    private final REST.Method[] allowedMethods;

    @Override
    public RestEndpoint getUnderlyingEndpoint() {
        return null;
    }

    @Override
    public EndpointHandler getHandler() {
        return null;
    }

    ServerEndpoints(REST.Method... allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @Override
    public REST.Method[] allowedMethods() {
        return allowedMethods;
    }
}
