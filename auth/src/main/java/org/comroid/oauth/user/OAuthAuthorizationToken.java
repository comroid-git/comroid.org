package org.comroid.oauth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.service.Service;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.mutatio.model.Ref;
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
    private static final Logger logger = LogManager.getLogger();
    public final Ref<Service> service = getComputedReference(SERVICE);
    public final Ref<UserAccount> account = getComputedReference(ACCOUNT);
    public final Ref<Permit.Set> scopes = getComputedReference(SCOPES);
    public final Ref<String> userAgent = getComputedReference(USER_AGENT);
    public final Ref<String> code = getComputedReference(CODE);

    public Service getService() {
        return service.assertion("service");
    }

    public UserAccount getAccount() {
        return account.assertion("user account");
    }

    public Permit.Set getScopes() {
        return scopes.assertion("scopes");
    }

    public String getUserAgent() {
        return userAgent.assertion("user agent");
    }

    public String getCode() {
        return code.assertion("code");
    }

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
        code = code.replace('+', 'x').replace('=', 'x').replace('/', 'x');
        logger.trace("Generated auth code: {}", code);
        return Base64.encodeBytes(code.getBytes());
    }
}
