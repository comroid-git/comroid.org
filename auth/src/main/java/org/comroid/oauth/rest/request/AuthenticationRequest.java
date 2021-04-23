package org.comroid.oauth.rest.request;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.user.Permit;
import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;

public class AuthenticationRequest extends DataContainerBase<AuthenticationRequest> {
    @RootBind
    public static final GroupBind<AuthenticationRequest> Type
            = new GroupBind<>(AuthServer.MASTER_CONTEXT, "authentication-request");
    public static final VarBind<AuthenticationRequest, String, String, String> RESPONSE_TYPE
            = Type.createBind("response_type")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<AuthenticationRequest, String, UUID, UUID> CLIENT_ID
            = Type.createBind("client_id")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString) // is always uuid here
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<AuthenticationRequest, String, URI, URI> REDIRECT_URI
            = Type.createBind("redirect_uri")
            .extractAs(StandardValueType.STRING)
            .andRemap(Polyfill::uri)
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<AuthenticationRequest, String, String[], Permit.Set> SCOPES
            = Type.createBind("scope")
            .extractAs(StandardValueType.STRING)
            .andRemap(str -> str.split(" "))
            .reformatRefs(refs -> Permit.valueOf(refs
                    .streamValues()
                    .flatMap(Stream::of)
                    .toArray(String[]::new)))
            .setRequired()
            .build();
    public static final VarBind<AuthenticationRequest, String, String, String> STATE
            = Type.createBind("state")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<String> responseType = getComputedReference(RESPONSE_TYPE);
    public final Ref<UUID> clientId = getComputedReference(CLIENT_ID);
    public final Ref<URI> redirectUri = getComputedReference(REDIRECT_URI);
    public final Ref<Permit.Set> scopes = getComputedReference(SCOPES);
    public final Ref<String> state = getComputedReference(STATE);

    {
        if (!responseType.contentEquals("code"))
            throw new IllegalArgumentException(String.format("Invalid response type '%s'; must be code", responseType.get()));
    }

    public UUID getClientID() {
        return clientId.assertion("client id");
    }

    public URI getRedirectURI() {
        return redirectUri.assertion("redirect_uri");
    }

    public Permit.Set getScopes() {
        return scopes.assertion("scope");
    }

    public @Nullable String getState() {
        return state.get();
    }

    public AuthenticationRequest(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
    }

    @Override
    public String toString() {
        return String.format("AuthenticationRequest{responseType=%s, clientId=%s, redirectUri=%s, scopes=%s, state=%s}",
                responseType.get(), clientId.get(), redirectUri.get(), scopes.get(), state.get());
    }
}
