package org.comroid.status.server.rest;

import com.sun.net.httpserver.Headers;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.status.DependenyObject.Adapters;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.status.server.StatusServer;
import org.comroid.status.server.TokenCore;
import org.comroid.status.server.entity.LocalService;
import org.comroid.status.server.util.ResponseBuilder;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;

import static org.comroid.restless.HTTPStatusCodes.*;

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
    },
    UPDATE_SERVICE_STATUS(Endpoint.UPDATE_SERVICE_STATUS, false) {
        @Override
        public REST.Response executePOST(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            final LocalService service = StatusServer.instance.getServiceByName(urlParams[0])
                    .map(LocalService.class::cast)
                    .orElseThrow(() -> new RestEndpointException(NOT_FOUND, "No local service found with name " + urlParams[0]));

            if (!headers.containsKey(CommonHeaderNames.AUTHORIZATION))
                throw new RestEndpointException(UNAUTHORIZED, "Unauthorized");

            final String token = headers.getFirst(CommonHeaderNames.AUTHORIZATION);

            if (token == null)
                throw new RestEndpointException(UNAUTHORIZED, "No Authorization Header found");

            if (!TokenCore.isValid(token) || !TokenCore.extractName(token).equals(service.getName()))
                throw new RestEndpointException(UNAUTHORIZED, "Malicious Token used");

            if (!service.getToken().equals(token))
                throw new RestEndpointException(UNAUTHORIZED, "Unauthorized");

            final Service.Status newStatus = body.process("status")
                    .map(UniNode::asInt)
                    .map(Service.Status::valueOf)
                    .wrap()
                    .orElseThrow(() -> new RestEndpointException(BAD_REQUEST, "No new status defined"));

            service.setStatus(newStatus);

            return new ResponseBuilder()
                    .setStatusCode(200)
                    .setBody(service.toObjectNode(body.getSerializationAdapter()))
                    .build();
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
