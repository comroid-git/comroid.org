package org.comroid.status.server.rest;

import org.comroid.common.io.FileHandle;
import org.comroid.status.entity.Service;
import org.comroid.status.entity.Service.Status;
import org.comroid.status.server.StatusServer;

import java.util.stream.Stream;

public enum StatusIcon {
    UNKNOWN(Status.UNKNOWN),

    OFFLINE(Status.OFFLINE),
    CRASHED(Status.CRASHED),
    MAINTENANCE(Status.MAINTENANCE),

    NOT_RESPONDING(Status.NOT_RESPONDING),

    ONLINE(Status.ONLINE);

    private final FileHandle icon;
    private final Status status;

    public FileHandle getIconFile() {
        return icon;
    }

    StatusIcon(Status status) {
        this.status = status;
        this.icon = StatusServer.DATA_DIR.createSubFile(String.format("status-%s.png", name().toLowerCase()));
    }

    public static StatusIcon valueOf(Status status) {
        return Stream.of(values())
                .filter(icon -> icon.status == status)
                .findAny()
                .orElse(UNKNOWN);
    }
}
