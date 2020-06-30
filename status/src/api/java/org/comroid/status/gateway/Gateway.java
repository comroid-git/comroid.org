package org.comroid.status.gateway;

import org.comroid.api.Polyfill;
import org.comroid.api.UUIDContainer;
import org.comroid.listnr.EventManager;
import org.comroid.listnr.ListnrCore;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebSocket;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.DependenyObject;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.trie.TrieMap;
import org.comroid.uniform.SerializationAdapter;

import java.util.Map;
import java.util.concurrent.Executor;

public final class Gateway extends UUIDContainer
        implements EventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload> {
    private final Map<String, Pipe<?, ? extends GatewayPayload>> pipes = TrieMap.ofString();
    private final ListnrCore listnrCore;
    private final WebSocket webSocket;

    public Gateway(HttpAdapter httpAdapter, SerializationAdapter<?, ?, ?> seriLib, Executor executor) {
        this.listnrCore = new ListnrCore(executor);

        REST.Header.List headers = new REST.Header.List();

        this.webSocket = httpAdapter.createWebSocket(
                seriLib,
                executor,
                DependenyObject.GATEWAY_URI,
                headers
        ).join();
    }

    @Override
    public ListnrCore listnr() {
        return listnrCore;
    }
}
