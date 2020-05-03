package org.comroid.status.entity;

import org.comroid.common.ref.IntEnum;

public enum EntityType implements IntEnum {
    SERVICE;

    @Override
    public int getValue() {
        return ordinal();
    }

    public static EntityType valueOf(int value) {
        return values()[value];
    }
}
