package org.comroid.status.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.varbind.VarCarrier;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ContextHandler implements HttpHandler {
    ALL_SERVICES(Endpoint.LIST_SERVICES, exchange -> {
        UniArrayNode services = UniArrayNode.ofList(
                StatusServer.instance.getSerializationLibrary(),
                StatusServer.instance.getEntityCache()
                        .stream()
                        .filter(ref -> ref.process()
                                .test(Service.class::isInstance))
                        .map(ref -> ref.process()
                                .map(Service.class::cast)
                                .map(VarCarrier::toObjectNode)
                                .get())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        return new REST.Response(HTTPStatusCodes.OK, services);
    });

    private final Endpoint endpoint;
    private final Function<HttpExchange, REST.Response> handler;

    ContextHandler(Endpoint endpoint, Function<HttpExchange, REST.Response> handler) {
        this.endpoint = endpoint;
        this.handler = handler;
    }

    public static void sortExchange(StatusServer server, HttpExchange exchange) throws IOException {
        final URI requestURI = exchange.getRequestURI();

        Optional<ContextHandler> handler = Stream.of(values())
                .filter(ctx -> ctx.endpoint.testURI(requestURI))
                .findFirst();

        if (handler.isPresent()) {
            handler.get()
                    .handle(exchange);
        } else {
            exchange.sendResponseHeaders(HTTPStatusCodes.NOT_FOUND, 0);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        final REST.Response response = handler.apply(exchange);
        final String data = response.getBody()
                .getBaseNode()
                .toString();

        for (char c : data.toCharArray()) {
            exchange.getResponseBody()
                    .write(c);
        }
        response.getHeaders()
                .forEach((name, value) -> exchange.getResponseHeaders()
                        .add(name, value));
        exchange.sendResponseHeaders(response.getStatusCode(), data.length());
    }
}
