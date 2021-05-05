package org.comroid.auth;

import org.apache.logging.log4j.LogManager;
import org.comroid.api.ContextualProvider;

public final class ComroidAuthServer {
    static {
        if (ContextualProvider.Base.ROOT.streamContextMembers(false).count() == 0)
            LogManager.getLogger().error("Warning: Root Context has not been properly initialized. Expect Errors");
    }

    private ComroidAuthServer() {
        throw new UnsupportedOperationException();
    }

    public static final String URL_BASE = "https://auth.comroid.org";
}
