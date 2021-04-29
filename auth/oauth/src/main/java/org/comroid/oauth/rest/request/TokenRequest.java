package org.comroid.oauth.rest.request;

import org.comroid.oauth.OAuth;
import org.comroid.oauth.model.GrantType;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public abstract class TokenRequest extends DataContainerBase<TokenRequest> {
    public static final GroupBind<TokenRequest> Type
            = new GroupBind<>(OAuth.CONTEXT, "token-request");
    public static final VarBind<TokenRequest, String, GrantType, GrantType> GRANT_TYPE
            = Type.createBind("grant_type")
            .extractAs(StandardValueType.STRING)
            .andRemap(name -> GrantType.valueOf(name.toUpperCase()))
            .onceEach()
            .setRequired()
            .build();
    public final Ref<GrantType> grantType = getComputedReference(GRANT_TYPE);

    public GrantType getGrantType() {
        return grantType.assertion("grant type");
    }

    protected TokenRequest(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
    }

    public static final class AuthorizationCodeGrant extends TokenRequest {
        @RootBind
        public static final GroupBind<AuthorizationCodeGrant> Type
                = TokenRequest.Type.subGroup("authorization-code-grant");
        public static final VarBind<AuthorizationCodeGrant, String, String, String> CODE
                = Type.createBind("code")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired()
                .build();
        public static final VarBind<AuthorizationCodeGrant, String, URI, URI> REDIRECT_URI
                = Type.createBind("redirect_uri")
                .extractAs(StandardValueType.STRING)
                .andRemap(Polyfill::uri)
                .onceEach()
                .setRequired()
                .build();
        public final Ref<String> code = getComputedReference(CODE);
        public final Ref<URI> redirectURI = getComputedReference(REDIRECT_URI);

        public String getCode() {
            return code.assertion("code");
        }

        public URI getRedirectURI() {
            return redirectURI.assertion("redirect uri");
        }

        public AuthorizationCodeGrant(ContextualProvider context, @Nullable UniObjectNode initialData) {
            super(context, initialData);

            if (!grantType.contentEquals(GrantType.AUTHORIZATION_CODE))
                throw new IllegalArgumentException("Illegal grant type: " + grantType.get());
        }
    }
}
