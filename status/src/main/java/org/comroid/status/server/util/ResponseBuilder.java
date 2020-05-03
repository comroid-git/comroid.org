package org.comroid.status.server.util;

import org.comroid.common.func.Builder;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;

public class ResponseBuilder implements Builder<REST.Response> {
    private final REST.Header.List list = new REST.Header.List();
    private final REST    restClient;

    public ResponseBuilder(REST restClient) {
        this.restClient = restClient;
    }

    public ResponseBuilder setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ResponseBuilder setBody(UniNode body) {
        this.body = body;
        return this;
    }

    public ResponseBuilder addHeader(String name, String value) {
        this.list.add(name, value);
        return this;
    }

    @Override
    public REST.Response build() {
        return new REST.Response(restClient, statusCode, body) {{
            getHeaders().addAll(list);
        }};
    }
    private       int     statusCode;
    private       UniNode body;
}
