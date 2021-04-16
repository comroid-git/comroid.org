package org.comroid.oauth.user;

import org.comroid.api.ContextualProvider;
import org.comroid.auth.server.AuthServer;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

public class OAuthBlob extends DataContainerBase<OAuthBlob> {
    public static final GroupBind<OAuthBlob> Type = new GroupBind<>(AuthServer.MASTER_CONTEXT, "oauth-blob");

    protected OAuthBlob(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
    }
}
