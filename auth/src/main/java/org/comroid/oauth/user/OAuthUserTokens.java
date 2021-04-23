package org.comroid.oauth.user;

import org.comroid.auth.server.AuthServer;
import org.comroid.auth.user.UserAccount;
import org.comroid.restless.MimeType;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import java.io.Closeable;

public final class OAuthUserTokens extends DataContainerBase<OAuthUserTokens> implements Closeable {
    @RootBind
    public static final GroupBind<OAuthUserTokens> Type
            = new GroupBind<>(AuthServer.MASTER_CONTEXT, "oauth-session");
    private final UserAccount userAccount;

    { // initialize tokens if nonpresent
    }

    public OAuthUserTokens(final Context context, final UserAccount userAccount) {
        super(context, obj -> {
            String content = userAccount.getDirectory().createSubFile("oauth.json").getContent();
            if (content.isEmpty())
                return;
            UniNode data = context.parse(MimeType.JSON, content);
            obj.copyFrom(data);
        });
        this.userAccount = userAccount;
    }
}
