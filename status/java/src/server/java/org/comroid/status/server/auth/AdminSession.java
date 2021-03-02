package org.comroid.status.server.auth;

import com.sun.net.httpserver.Headers;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.status.server.StatusServer;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.comroid.restless.CommonHeaderNames.AUTHORIZATION;
import static org.comroid.restless.CommonHeaderNames.USER_AGENT;
import static org.comroid.restless.HTTPStatusCodes.*;
import static org.comroid.status.server.auth.TokenCore.extractName;
import static org.comroid.status.server.auth.TokenCore.generate;

public final class AdminSession extends DataContainerBase<AdminSession> implements UncheckedCloseable {
    @RootBind
    public static final GroupBind<AdminSession> TYPE
            = new GroupBind<>(StatusServer.instance, "admin-session");
    public static final VarBind<AdminSession, String, UUID, UUID> ID
            = TYPE.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .build();
    public static final VarBind<AdminSession, String, String, String> TOKEN
            = TYPE.createBind("token")
            .extractAs(StandardValueType.STRING)
            .build();

    private static final Map<String, AdminSession> sessions = new ConcurrentHashMap<>();
    public final Reference<UUID> id = getComputedReference(ID);
    public final Reference<String> token = getComputedReference(TOKEN);

    public UUID getId() {
        return id.assertion("ID nonexistent");
    }

    private AdminSession(String userAgent, String token) {
        super(StatusServer.instance, null /*todo: add UniNode Builders*/);
        put(ID, UUID.randomUUID().toString());
        String myToken = generate(userAgent + ':' + token + ':' + getId());
        String[] split = extractName(myToken).split(":");
        if (!split[0].equals(userAgent) || !split[2].equals(getId().toString()))
            throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Generated session Token was invalid");
        put(TOKEN, myToken);

        sessions.put(token, this);
    }

    public static REST.Response create(Headers headers) throws RestEndpointException {
        String token = headers.getFirst(AUTHORIZATION);
        String agent = headers.getFirst(USER_AGENT);
        assert token != null : "token";
        if (agent == null)
            throw new RestEndpointException(UNAUTHORIZED, "UserAgent required");
        return new AdminSession(agent, token).infoResponse();
    }

    @Nullable
    public static AdminSession findSession(String sessionToken) {
        return sessions.getOrDefault(sessionToken, null);
    }

    public static boolean deleteSession(@Nullable String token) throws RestEndpointException {
        if (token == null)
            throw new RestEndpointException(UNAUTHORIZED);
        AdminSession session = findSession(token);
        if (session == null)
            return false;
        session.close();
        return true;
    }

    public REST.Response infoResponse() {
        return new REST.Response(OK, toUniNode());
    }

    @Override
    public void close() {
    }
}
