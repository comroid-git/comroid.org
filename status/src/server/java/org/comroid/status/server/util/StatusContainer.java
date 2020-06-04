package org.comroid.status.server.util;

import org.comroid.status.entity.Service;

public interface StatusContainer {
    Service.Status getStatus();

    void setStatus(Service.Status status);
}
