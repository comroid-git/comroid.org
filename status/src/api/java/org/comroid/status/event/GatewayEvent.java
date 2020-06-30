package org.comroid.status.event;

import org.comroid.listnr.EventType;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.gateway.GatewayOpCode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.function.Function;

public interface GatewayEvent<P extends GatewayPayload> extends EventType<WebSocketPayload.Data, P> {
    GatewayEvent<GatewayPayload.Heartbeat> HEARTBEAT
            = new Base<>("gateway-heartbeat", GatewayOpCode.HEARTBEAT, GatewayPayload.Heartbeat::new);

    final class Base<P extends GatewayPayload> implements GatewayEvent<P> {
        private final String name;
        private final int opCode;
        private final Function<UniObjectNode, P> remapper;

        @Override
        public String getName() {
            return name;
        }

        public Base(String name, GatewayOpCode opCode, Function<UniObjectNode, P> remapper) {
            this.name = name;
            this.opCode = opCode.getValue();
            this.remapper = remapper;
        }

        @Override
        public boolean test(WebSocketPayload.Data data) {
            return data.getBody()
                    .get("op")
                    .asInt(-1) == opCode;
        }

        @Override
        public P apply(WebSocketPayload.Data data) {
            return remapper.apply(data.getBody()
                    .get("data")
                    .asObjectNode());
        }
    }
}
