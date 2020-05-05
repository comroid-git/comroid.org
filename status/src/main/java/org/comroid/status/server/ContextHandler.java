package org.comroid.status.server;

import com.sun.net.httpserver.HttpExchange;
import org.comroid.restless.REST;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.varbind.container.DataContainer;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.restless.HTTPStatusCodes.NOT_FOUND;
import static org.comroid.restless.HTTPStatusCodes.OK;

public enum ContextHandler {
    ALL_SERVICES(Endpoint.LIST_SERVICES, (exchange, args) -> {
        UniArrayNode services = UniArrayNode.ofList(
                StatusServer.instance.getSerializationLibrary(),
                StatusServer.instance.getEntityCache()
                        .stream()
                        .filter(ref -> ref.process()
                                .test(Service.class::isInstance))
                        .flatMap(ref -> ref.process()
                                .map(Service.class::cast)
                                .map(DataContainer::toObjectNode)
                                .flatMap(Stream::of))
                        .collect(Collectors.toList())
        );

        return new REST.Response(OK, services);
    }),
    GET_SERVICE(Endpoint.GET_SERVICE, (exchange, args) -> {
        Service service = StatusServer.instance.getServiceByID(UUID.fromString(args[0]))
                .orElse(null);

        if (service == null)
            return REST.Response.empty(StatusServer.instance.getSerializationLibrary(), NOT_FOUND);
        else return new REST.Response(OK, service.toObjectNode());
    });

    private final Endpoint endpoint;
    private final BiFunction<HttpExchange, String[], REST.Response> handler;

    ContextHandler(Endpoint endpoint, BiFunction<HttpExchange, String[], REST.Response> handler) {
        this.endpoint = endpoint;
        this.handler = handler;
    }

    public static void sortExchange(StatusServer server, HttpExchange exchange) throws IOException {
        final URI requestURI = exchange.getRequestURI();

        Optional<ContextHandler> handler = Stream.of(values())
                .filter(ctx -> ctx.endpoint.test(requestURI))
                .findFirst();

        if (handler.isPresent()) {
            final ContextHandler ctx = handler.get();
            final String[] args = ctx.endpoint.extractArgs(requestURI);

            ctx.handle(exchange, args);
        } else {
            exchange.sendResponseHeaders(NOT_FOUND, 0);
        }
    }

    public void handle(HttpExchange exchange, String[] args) throws IOException {
        final REST.Response response = handler.apply(exchange, args);
        final String data = response.getBody()
                .getBaseNode()
                .toString();

        for (char c : data.toCharArray()) {
            exchange.getResponseBody()
                    .write(c);
        }

        response.getHeaders().forEach((name, value) ->
                exchange.getResponseHeaders().add(name, value));
        exchange.sendResponseHeaders(response.getStatusCode(), data.length());
    }
}
