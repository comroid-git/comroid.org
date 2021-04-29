package org.comroid.oauth.rest.request;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.Ref;
import org.comroid.oauth.OAuth;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuthenticationRequest extends DataContainerBase<AuthenticationRequest> {
    public static final String SCOPE_SPLIT_PATTERN = "[\\s+]";
    @RootBind
    public static final GroupBind<AuthenticationRequest> Type
            = new GroupBind<>(OAuth.CONTEXT, "authentication-request");
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
    public static final VarBind<AuthenticationRequest, String, String[], Set<String>> SCOPES
            = Type.createBind("scope")
            .extractAs(StandardValueType.STRING)
            .andRemap(str -> str.split(SCOPE_SPLIT_PATTERN))
            .reformatRefs(refs -> Collections.unmodifiableSet(refs
                    .streamValues()
                    .flatMap(Stream::of)
                    .collect(Collectors.toSet())))
            .setRequired()
            .build();
    public static final VarBind<AuthenticationRequest, String, String, String> STATE
            = Type.createBind("state")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<String> responseType = getComputedReference(RESPONSE_TYPE);
    public final Ref<UUID> clientId = getComputedReference(CLIENT_ID);
    public final Ref<URI> redirectUri = getComputedReference(REDIRECT_URI);
    public final Ref<Set<String>> scopes = getComputedReference(SCOPES);
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

    public Set<String> getScopes() {
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
