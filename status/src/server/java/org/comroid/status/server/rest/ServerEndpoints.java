package org.comroid.status.server.rest;

import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.RestEndpoint;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.status.DependenyObject;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.util.ResponseBuilder;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.Optional;

import static org.comroid.restless.HTTPStatusCodes.NOT_FOUND;
import static org.comroid.status.DependenyObject.Adapters.SERIALIZATION_ADAPTER;

public enum ServerEndpoints implements ServerEndpoint.Underlying {
    LIST_SERVICES(Endpoint.LIST_SERVICES, (data, args) -> {
        if (args.length != 0)
            throw new IllegalArgumentException("Invalid argument count");

        final UniArrayNode services = SERIALIZATION_ADAPTER.createUniArrayNode(null);

        StatusServer.instance
                .getEntityCache()
                .stream()
                .filter(ref -> ref.test(Service.class::isInstance))
                .map(ref -> ref.into(Service.class::cast))
                .forEach(service -> service.toObjectNode(services.addObject()));

        return new ResponseBuilder()
                .setStatusCode(200)
                .setBody(services)
                .build();
    }, REST.Method.GET),

    SERVICE_STATUS(Endpoint.SERVICE_STATUS, (data, args) -> {
        if (args.length != 1)
            throw new IllegalArgumentException("Invalid argument count");

        final UniObjectNode status = SERIALIZATION_ADAPTER.createUniObjectNode(null);

        final Optional<Service> serviceOpt = StatusServer.instance.getServiceByName(args[0]);

        if (!serviceOpt.isPresent())
            return REST.Response.empty(SERIALIZATION_ADAPTER, NOT_FOUND);

        serviceOpt.ifPresent(service -> {
            status.put("name", ValueType.STRING, service.getName());
            status.put("status", ValueType.INTEGER, service.getStatus().getValue());
        });

        return new ResponseBuilder()
                .setStatusCode(200)
                .setBody(status)
                .build();
    }, REST.Method.GET);

    private final Endpoint underlying;
    private final EndpointHandler handler;
    private final REST.Method[] allowedMethods;

    @Override
    public RestEndpoint getUnderlyingEndpoint() {
        return underlying;
    }

    @Override
    public EndpointHandler getHandler() {
        return handler;
    }

    ServerEndpoints(Endpoint underlying, EndpointHandler handler, REST.Method... allowedMethods) {
        this.underlying = underlying;
        this.handler = handler;
        this.allowedMethods = allowedMethods;
    }

    @Override
    public REST.Method[] allowedMethods() {
        return allowedMethods;
    }
}
