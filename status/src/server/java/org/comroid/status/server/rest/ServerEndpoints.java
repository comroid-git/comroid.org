package org.comroid.status.server.rest;

import com.sun.net.httpserver.Headers;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.status.DependenyObject.Adapters;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.util.ResponseBuilder;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;

import static org.comroid.restless.HTTPStatusCodes.NOT_FOUND;

public enum ServerEndpoints implements ServerEndpoint {
    LIST_SERVICES(Endpoint.LIST_SERVICES, false) {
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

    SPECIFIC_SERVICE(Endpoint.SPECIFIC_SERVICE, true) {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return StatusServer.instance.getServiceByName(urlParams[0])
                    .map(service -> service.toObjectNode(Adapters.SERIALIZATION_ADAPTER))
                    .map(node -> new ResponseBuilder()
                            .setStatusCode(200)
                            .setBody(node)
                            .build())
                    .orElseThrow(() -> new RestEndpointException(NOT_FOUND, "No service found with name " + urlParams[0]));
        }
    };

    private final Endpoint underlying;
    private final boolean allowMemberAccess;

    @Override
    public AccessibleEndpoint getEndpointBase() {
        return underlying;
    }


    ServerEndpoints(Endpoint underlying, boolean allowMemberAccess) {
        this.underlying = underlying;
        this.allowMemberAccess = allowMemberAccess;
    }

    @Override
    public boolean allowMemberAccess() {
        return allowMemberAccess;
    }
}
