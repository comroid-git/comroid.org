package org.comroid.oauth;

public final class OAuth {
    private OAuth() {
        throw new UnsupportedOperationException();
    }

    public interface User {
        String getId();
    }

    public interface Service {
    }
}
