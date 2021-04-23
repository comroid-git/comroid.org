package org.comroid.oauth.model;

import org.comroid.api.Named;

public enum GrantType implements Named {
    AUTHORIZATION_CODE, PASSWORD, CLIENT_CREDENTIALS, REFRESH_TOKEN
}
