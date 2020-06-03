package org.comroid.status.server.util;

import org.comroid.common.func.Builder;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;

public class ResponseBuilder implements Builder<REST.Response> {
    private final REST.Header.List list = new REST.Header.List();

    public ResponseBuilder() {
        list.add("Access-Control-Allow-Origin", "*");
        list.add(CommonHeaderNames.ACCEPTED_CONTENT_TYPE, "application/json");
        list.add(CommonHeaderNames.REQUEST_CONTENT_TYPE, "application/json");
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
        return new REST.Response(statusCode, body) {{
            getHeaders().addAll(list);
        }};
    }
    private       int     statusCode;
    private       UniNode body;
}
