package org.comroid.oauth.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.StreamSupplier;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.service.Service;
import org.comroid.auth.service.ServiceManager;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.auth.user.UserManager;
import org.comroid.auth.user.UserSession;
import org.comroid.oauth.model.OAuthError;
import org.comroid.oauth.rest.request.AuthenticationRequest;
import org.comroid.oauth.rest.request.TokenRequest;
import org.comroid.oauth.user.OAuthAuthorization;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.body.URIQueryEditor;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.intellij.lang.annotations.Language;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.comroid.restless.HTTPStatusCodes.*;

// fixme fixme fixme
public enum OAuthEndpoint implements ServerEndpoint.This {
    AUTHORIZE("/authorize") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            AuthenticationRequest authenticationRequest = new AuthenticationRequest(context, body.asObjectNode());
            URI redirectURI = authenticationRequest.getRedirectURI();
            URIQueryEditor query = new URIQueryEditor(redirectURI);
            logger.debug("Got {}", authenticationRequest);

            // find service by client id
            final ServiceManager serviceManager = context.requireFromContext(ServiceManager.class);
            final UUID clientID = authenticationRequest.getClientID();
            final Service service = serviceManager.getService(clientID)
                    .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED, "Service with ID " + clientID + " not found"));
            final String userAgent = headers.getFirst(CommonHeaderNames.USER_AGENT);

            try {
                // find session & account
                final UserSession session = UserSession.findSession(headers);
                final UserAccount account = session.getAccount();

                String authorizationCode = completeAuthorization(account, authenticationRequest, context, service, userAgent);

                // assemble redirect uri
                query.put("code", authorizationCode);
                if (authenticationRequest.state.isNonNull())
                    query.put("state", authenticationRequest.getState());
            } catch (RestEndpointException e) {
                if (e.getStatusCode() != UNAUTHORIZED)
                    query.put("error", OAuthError.SERVER_ERROR.getValue());
                else {
                    // send frame and obtain session from there
                    Map<String, Object> pageProps = context.requireFromContext(PagePropertiesProvider.class)
                            .findPageProperties(headers);
                    FrameBuilder frame = new FrameBuilder("quickAction", headers, pageProps, false);
                    frame.setPanel("flowLogin");

                    UUID requestId = UUID.randomUUID();
                    loginRequests.put(requestId, authenticationRequest);
                    Map<String, Object> flow = new HashMap<>();
                    flow.put("requestId", requestId);
                    flow.put("resourceName", service.getName());
                    flow.put("scopes", authenticationRequest.getScopes());
                    frame.setProperty("flow", flow);

                    return new REST.Response(OK, "text/html", frame.toReader());
                }
            } catch (Exception e) {
                logger.warn("Could not authorize OAuth session; aborting", e);

                // fixme use correct codes
                query.put("error", OAuthError.SERVER_ERROR.getValue());
            }

            return new REST.Response(HTTPStatusCodes.FOUND, query.toURI());
        }
    },
    AUTHORIZE_LOGIN("/authorize/login") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            String email = body.get("email").asString(str -> str.replace("%40", "@"));
            String login = body.get("password").asString();

            UUID requestId = body.get("requestId").asString(UUID::fromString);
            AuthenticationRequest authenticationRequest = loginRequests.getOrDefault(requestId, null);
            URIQueryEditor query = new URIQueryEditor(authenticationRequest.getRedirectURI());

            UserSession session;
            try {
                session = AuthServer.instance.getUserManager().loginUser(email, login);
            } catch (RestEndpointException e) {
                query.put("error", OAuthError.UNAUTHORIZED_CLIENT.getValue());
                return new REST.Response(FOUND, query.toURI());
            }

            UUID clientID = authenticationRequest.getClientID();
            Service service = AuthServer.instance.getServiceManager().getService(clientID)
                    .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED, "Service with ID " + clientID + " not found"));
            String userAgent = headers.getFirst(CommonHeaderNames.USER_AGENT);

            String code = OAuthEndpoint.completeAuthorization(session.getAccount(), authenticationRequest, context, service, userAgent);

            // assemble redirect uri
            query.put("code", code);
            if (authenticationRequest.state.isNonNull())
                query.put("state", authenticationRequest.getState());

            return new REST.Response(FOUND, query.toURI());
        }
    },
    TOKEN("/token") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            TokenRequest.AuthorizationCodeGrant tokenRequest = new TokenRequest.AuthorizationCodeGrant(context, body.asObjectNode());
            OAuthAuthorization authorization = context.requireFromContext(UserManager.class)
                    .findOAuthAuthorization(tokenRequest.getCode());
            OAuthAuthorization.AccessToken accessToken = authorization.createAccessToken();

            return new REST.Response(OK, accessToken);
        }
    },
    USER_INFO("/userInfo") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UserAccount account = context.requireFromContext(UserManager.class).findOAuthSession(headers);
            return new REST.Response(OK, account);
        }

        @Override
        public boolean allowMemberAccess() {
            return true;
        }
    };

    public static final StreamSupplier<ServerEndpoint> values = StreamSupplier.of(values());
    private static final Map<UUID, AuthenticationRequest> loginRequests = new ConcurrentHashMap<>();
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

    private static String completeAuthorization(UserAccount account, AuthenticationRequest request, Context context, Service service, String userAgent) {
        // validate account has scopes as permit
        account.checkPermits(request.getScopes().toArray(new Permit[0]));

        // create oauth blob for user with this service + user agent
        OAuthAuthorization authorization = account.createOAuthSession(context, service, userAgent, request.getScopes());
        return authorization.getCode();
    }
}
