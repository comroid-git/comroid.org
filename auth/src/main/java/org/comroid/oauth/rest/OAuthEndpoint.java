package org.comroid.oauth.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.StreamSupplier;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.service.Service;
import org.comroid.auth.service.ServiceManager;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.auth.user.UserSession;
import org.comroid.common.info.MessageSupplier;
import org.comroid.oauth.model.OAuthError;
import org.comroid.oauth.rest.request.AuthenticationRequest;
import org.comroid.oauth.user.OAuthAuthorizationToken;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.body.URIQueryEditor;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.intellij.lang.annotations.Language;

import java.util.UUID;
import java.util.regex.Pattern;

public enum OAuthEndpoint implements ServerEndpoint.This {
    AUTHORIZE("/authorize") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            // fixme
            logger.debug("Received authorization request with body:\n{}", body.toSerializedString());

            AuthenticationRequest authenticationRequest = new AuthenticationRequest(context, body.asObjectNode());
            URIQueryEditor query = new URIQueryEditor(authenticationRequest.getRedirectURI());
            logger.debug("Got {}", authenticationRequest);

            try {
                // find session & account
                final UserSession session = UserSession.findSession(headers);
                final UserAccount account = session.getAccount();
                // validate account has scopes as permit
                account.checkPermits(authenticationRequest.getScopes()
                        .toArray(new Permit[0]));

                // find service by client id
                final ServiceManager serviceManager = context.requireFromContext(ServiceManager.class);
                final UUID clientID = authenticationRequest.getClientID();
                final Service service = serviceManager.getService(clientID)
                        .requireNonNull(MessageSupplier.format("Could not find Service by Client ID %s", clientID));
                final String userAgent = headers.getFirst(CommonHeaderNames.USER_AGENT);

                // create oauth blob for user with this service + user agent
                OAuthAuthorizationToken authorization = account.createOAuthSession(context, service, userAgent);

                // assemble redirect uri
                query.put("code", authorization.getCode());
                if (authenticationRequest.state.isNonNull())
                    query.put("state", authenticationRequest.getState());
            } catch (Exception e) {
                logger.warn("Could not authorize OAuth session; aborting", e);

                // fixme use correct codes
                query.put("error", OAuthError.SERVER_ERROR.getValue());
            }

            return new REST.Response(HTTPStatusCodes.FOUND, query.toURI());
        }
    },
    TOKEN("/token") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            // fixme
            logger.debug("Received token request with body:\n{}", body.toSerializedString());
            return null;
        }
    };

    public static final StreamSupplier<ServerEndpoint> values = StreamSupplier.of(values());
    private static final Logger logger = LogManager.getLogger();
    private final String extension;
    private final String[] regExp;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AuthServer.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return "/oauth2" + extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    OAuthEndpoint(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
        this.pattern = buildUrlPattern();
    }
}
