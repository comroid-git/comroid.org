package org.comroid.status.server.rest;

import com.sun.net.httpserver.Headers;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.status.DependenyObject.Adapters;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.util.ResponseBuilder;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.Optional;

import static org.comroid.restless.HTTPStatusCodes.NOT_FOUND;

public enum ServerEndpoints implements ServerEndpoint {
    LIST_SERVICES(Endpoint.LIST_SERVICES) {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            final UniArrayNode services = Adapters.SERIALIZATION_ADAPTER.createUniArrayNode();

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
        }
    },

    SERVICE_STATUS(Endpoint.SERVICE_STATUS) {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            final UniObjectNode status = Adapters.SERIALIZATION_ADAPTER.createUniObjectNode();

            final Optional<Service> serviceOpt = StatusServer.instance.getServiceByName(urlParams[0]);

            if (!serviceOpt.isPresent())
                return new ResponseBuilder()
                        .setStatusCode(NOT_FOUND)
                        .setBody(status)
                        .build();

            serviceOpt.ifPresent(service -> {
                status.put("name", ValueType.STRING, service.getName());
                status.put("status", ValueType.INTEGER, service.getStatus().getValue());
            });

            return new ResponseBuilder()
                    .setStatusCode(200)
                    .setBody(status)
                    .build();
        }
    };

    private final Endpoint underlying;

    @Override
    public AccessibleEndpoint getEndpointBase() {
        return underlying;
    }


    ServerEndpoints(Endpoint underlying) {
        this.underlying = underlying;
    }
}
