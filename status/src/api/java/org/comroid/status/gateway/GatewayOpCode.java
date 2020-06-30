package org.comroid.status.gateway;

import org.comroid.api.IntEnum;

public enum GatewayOpCode implements IntEnum {
    HEARTBEAT(0),
    HEARTBEAT_ACK(10);

    private final int value;

    @Override
    public int getValue() {
        return value;
    }

    GatewayOpCode(int value) {
        this.value = value;
    }
}
