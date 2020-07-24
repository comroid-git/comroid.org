package org.comroid.status.server.rest;

import org.comroid.common.io.FileHandle;
import org.comroid.status.entity.Service;
import org.comroid.status.server.StatusServer;

import java.util.stream.Stream;

public enum StatusIcon {
    UNKNOWN(Service.Status.UNKNOWN),

    OFFLINE(Service.Status.OFFLINE),
    MAINTENANCE(Service.Status.MAINTENANCE),
    REPORTED_PROBLEMS(Service.Status.REPORTED_PROBLEMS),
    ONLINE(Service.Status.ONLINE);

    private final FileHandle icon;
    private final Service.Status status;

    public FileHandle getIconFile() {
        return icon;
    }

    StatusIcon(Service.Status status) {
        this.status = status;
        this.icon = StatusServer.DATA_DIR.createSubFile(String.format("status-%s.png", name().toLowerCase()));
    }

    public static StatusIcon valueOf(Service.Status status) {
        return Stream.of(values())
                .filter(icon -> icon.status == status)
                .findAny()
                .orElse(UNKNOWN);
    }
}
