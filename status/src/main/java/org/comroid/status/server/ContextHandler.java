package org.comroid.status.server;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.comroid.restless.REST;
import org.comroid.status.entity.Service;
import org.comroid.status.server.util.ResponseBuilder;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.VarCarrier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public enum ContextHandler implements HttpHandler {
    ALL_SERVICES(exchange -> {
        StatusServer.instance.getEntityCache()
                .stream()
                .filter(ref -> ref.process()
                        .test(Service.class::isInstance))
                .map(ref -> ref.process()
                        .map(Service.class::cast)
                        .map(VarCarrier::toObjectNode)
                        .map(UniObjectNode::toString)
                        .get())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", ", "[", "]"));

        // todo
    });

    private final Function<HttpExchange, REST.Response> handler;

    ContextHandler(Function<HttpExchange, REST.Response> handler) {
        this.handler = handler;
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
