package org.comroid.oauth.resource;

import org.comroid.api.Rewrapper;

import java.util.UUID;

public interface ResourceProvider {
    boolean hasResource(UUID uuid);

    Rewrapper<? extends Resource> getResource(UUID uuid);
}
