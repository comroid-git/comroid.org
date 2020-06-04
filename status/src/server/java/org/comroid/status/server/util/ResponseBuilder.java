package org.comroid.status.server.util;

import org.comroid.common.func.Builder;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;

public class ResponseBuilder implements Builder<REST.Response> {
    private final REST.Header.List headers = new REST.Header.List();
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
