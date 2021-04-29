package org.comroid.oauth.client;

import org.comroid.api.Rewrapper;
import org.comroid.oauth.user.OAuthAuthorization;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;

import java.util.UUID;

public interface ClientProvider {
    OAuthAuthorization findAuthorization(String authorizationCode) throws RestEndpointException;

    OAuthAuthorization.AccessToken findAccessToken(REST.Header.List headers) throws RestEndpointException;

    boolean hasClient(UUID uuid);

    Rewrapper<? extends Client> findClient(UUID uuid);
}
