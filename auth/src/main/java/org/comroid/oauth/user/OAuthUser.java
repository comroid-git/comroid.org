package org.comroid.oauth.user;

import org.comroid.api.ContextualProvider;
import org.comroid.oauth.OAuth;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.Nullable;

public class OAuthUser extends OAuthBlob implements OAuth.User {
    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    public OAuthUser(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
    }
}
