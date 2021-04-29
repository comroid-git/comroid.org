package org.comroid.oauth;

import org.comroid.api.ContextualProvider;
import org.comroid.uniform.Context;

public final class OAuth {
    public static ContextualProvider CONTEXT;

    private OAuth() {
        throw new UnsupportedOperationException();
    }

    public interface User {
        String getId();
    }

    public interface Service {
    }
}
