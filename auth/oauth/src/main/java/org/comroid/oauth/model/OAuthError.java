package org.comroid.oauth.model;

import org.comroid.api.Named;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.util.StandardValueType;

public enum OAuthError implements Named, ValueBox<String> {
    INVALID_REQUEST,
    UNAUTHORIZED_CLIENT,
    ACCESS_DENIED,
    UNSUPPORTED_RESPONSE_TYPE,
    INVALID_SCOPE,
    SERVER_ERROR,
    TEMPORARILY_UNAVAILABLE;

    @Override
    public String getValue() {
        return name().toLowerCase();
    }

    @Override
    public ValueType<? extends String> getHeldType() {
        return StandardValueType.STRING;
    }
}
