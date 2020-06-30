package org.comroid.status.event;

import org.comroid.listnr.EventPayload;
import org.comroid.uniform.node.UniObjectNode;

public interface GatewayPayload extends EventPayload {
    abstract class Abstract implements GatewayPayload {
    }

    final class Heartbeat extends Abstract {
        private final int offset;

        public int getOffset() {
            return offset;
        }

        protected Heartbeat(UniObjectNode data) {
            this.offset = data.get("offset").asInt();
        }
    }
}
