package org.comroid.oauth.user;

import org.comroid.auth.server.AuthServer;
import org.comroid.auth.service.Service;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.uniform.Context;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.java_websocket.util.Base64;

import java.util.UUID;
import java.util.stream.Stream;

public final class OAuthAuthorizationToken extends DataContainerBase<OAuthAuthorizationToken> {
    @RootBind
    public static final GroupBind<OAuthAuthorizationToken> Type
            = new GroupBind<>(AuthServer.MASTER_CONTEXT, "oauth-session");
    public static final VarBind<OAuthAuthorizationToken, String, Service, Service> SERVICE
            = Type.createBind("service_id")
            .extractAs(StandardValueType.STRING)
            .andRemapRef(uuid -> AuthServer.instance.getServiceManager()
                    .getService(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorizationToken, String, UserAccount, UserAccount> ACCOUNT
            = Type.createBind("account_id")
            .extractAs(StandardValueType.STRING)
            .andRemapRef(uuid -> AuthServer.instance.getUserManager()
                    .getUser(UUID.fromString(uuid)))
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorizationToken, String, String[], Permit.Set> SCOPES
            = Type.createBind("scope")
            .extractAs(StandardValueType.STRING)
            .andRemap(str -> str.split(" "))
            .reformatRefs(refs -> Permit.valueOf(refs
                    .streamValues()
                    .flatMap(Stream::of)
                    .toArray(String[]::new)))
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorizationToken, String, String, String> USER_AGENT
            = Type.createBind("user_agent")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<OAuthAuthorizationToken, String, String, String> CODE
            = Type.createBind("code")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();

    public OAuthAuthorizationToken(Context context, final UserAccount userAccount, final Service service, final String userAgent) {
        super(context, obj -> {
            obj.put(ACCOUNT, userAccount.getUUID().toString());
            obj.put(SERVICE, service.getUUID().toString());
            obj.put(USER_AGENT, userAgent);
            obj.put(CODE, generateCode(userAccount, service, userAgent));
        });
    }

    private static String generateCode(UserAccount userAccount, Service service, String userAgent) {
        String code = String.format("%s-%s", userAccount.getUUID(), service.getUUID());
        code = UserAccount.encrypt(code, code + '-' + UUID.randomUUID());
        return Base64.encodeBytes(code.getBytes());
    }
}
