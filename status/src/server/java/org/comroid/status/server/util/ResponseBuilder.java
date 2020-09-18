package org.comroid.status.server.util;

import org.comroid.api.Builder;
import org.comroid.restless.REST;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.varbind.container.DataContainer;

public class ResponseBuilder implements Builder<REST.Response> {
    private final REST.Header.List headers = new REST.Header.List();
    private final SerializationAdapter<?, ?, ?> serilib;
    private int statusCode;
    private UniNode body;

    public ResponseBuilder setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ResponseBuilder setBody(UniNode body) {
        this.body = body;
        return this;
    }

    public ResponseBuilder setBody(DataContainer<?> body) {
        return setBody(body.toObjectNode(serilib.createUniObjectNode()));
    }

    public ResponseBuilder() {
        this(null);
    }

    public ResponseBuilder(SerializationAdapter<?, ?, ?> serializationAdapter) {
        this.serilib = serializationAdapter;
    }

    public ResponseBuilder addHeader(String name, String value) {
        this.headers.add(name, value);
        return this;
    }

    @Override
    public REST.Response build() {
        return new REST.Response(statusCode, body) {{
            getHeaders().addAll(headers);
        }};
    }
}
