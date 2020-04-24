package org.comroid.server.status;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.comroid.restless.HTTPStatusCodes;
import org.comroid.server.status.entity.message.StatusUpdateMessage;
import org.comroid.uniform.node.UniNode;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import static org.comroid.uniform.adapter.json.fastjson.FastJSONLib.fastJsonLib;

public final class HandlerContainer {
    public final  HttpHandler  HELLO;
    public final  HttpHandler  STATUS_UPDATE;
    private final StatusServer statusServer;

    public HandlerContainer(StatusServer statusServer) {
        this.statusServer = statusServer;

        HELLO         = httpExchange -> {
            httpExchange.getResponseHeaders()
                    .add("Accept",
                            statusServer.getSerializationLibrary()
                                    .getMimeType()
                    );

            if (validateContentType(httpExchange)) {
                httpExchange.sendResponseHeaders(HTTPStatusCodes.OK, 0);
            } else {
                httpExchange.sendResponseHeaders(HTTPStatusCodes.UNSUPPORTED_MEDIA_TYPE, 0);
            }
        };
        STATUS_UPDATE = httpExchange -> {
            if (!validateContentType(httpExchange)) {
                httpExchange.sendResponseHeaders(HTTPStatusCodes.UNSUPPORTED_MEDIA_TYPE, 0);
                return;
            }

            final String body = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody())).lines()
                    .collect(Collectors.joining());
            final UniNode data = statusServer.getSerializationLibrary()
                    .createUniNode(body);
            final StatusUpdateMessage message = new StatusUpdateMessage(statusServer, data.asObjectNode());
        };
    }

    private boolean validateContentType(HttpExchange exchange) {
        return exchange.getRequestHeaders()
                .getFirst("Content-Type")
                .equals(fastJsonLib.getMimeType());
    }
}
