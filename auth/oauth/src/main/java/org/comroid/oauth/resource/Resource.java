package org.comroid.oauth.resource;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.oauth.user.OAuthAuthorization;

public interface Resource extends UUIDContainer, Named {
    String generateAccessToken(OAuthAuthorization authorization);
}
