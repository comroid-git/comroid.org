package org.comroid.auth.user;

import org.comroid.api.BitmaskEnum;

import java.util.Set;

public enum Permit implements BitmaskEnum<Permit> {
    NONE, DEV, ADMIN;

    public static Set<Permit> valueOf(int mask) {
        return BitmaskEnum.valueOf(mask, Permit.class);
    }
}
