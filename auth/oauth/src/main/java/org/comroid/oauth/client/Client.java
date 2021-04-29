package org.comroid.oauth.client;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.common.io.FileHandle;
import org.comroid.oauth.resource.Resource;
import org.comroid.oauth.user.OAuthAuthorization;
import org.comroid.uniform.node.UniNode;

public interface Client extends UUIDContainer, Named {
    String generateAuthorizationToken(Resource resource, String userAgent);

    boolean addAccessToken(OAuthAuthorization.AccessToken accessToken);

    UniNode getUserInfo();

    FileHandle getDataDirectory();
}
