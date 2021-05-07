package org.comroid.auth.server;

import org.comroid.api.EMailAddress;
import org.comroid.api.Polyfill;
import org.comroid.api.StreamSupplier;
import org.comroid.auth.rest.AuthEndpoint;
import org.comroid.auth.service.FileBasedService;
import org.comroid.auth.service.Service;
import org.comroid.auth.service.ServiceManager;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserAccount;
import org.comroid.auth.user.UserDataStorage;
import org.comroid.auth.user.UserSession;
import org.comroid.mutatio.model.Ref;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.MimeType;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.oauth.client.ClientProvider;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.comroid.auth.user.UserAccount.EMAIL;
import static org.comroid.restless.HTTPStatusCodes.*;

public enum AuthServerEndpoint implements ServerEndpoint.This {
    FAVICON("/favicon.ico") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("https://cdn.comroid.org/favicon.ico"));
        }
    },
    WIDGET("/widget") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            Map<String, Object> pageProperties = context.requireFromContext(PagePropertiesProvider.class)
                    .findPageProperties(headers);
            FrameBuilder frame = new FrameBuilder("widget", new REST.Header.List(), pageProperties, false);

            return new REST.Response(OK, "text/html", frame.toReader());
        }
    },
    MODIFY_ACCOUNT(org.comroid.auth.rest.AuthEndpoint.MODIFY_ACCOUNT) {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                UserSession session = UserSession.findSession(headers);
                UserAccount account = session.getAccount();

                if (!account.getUUID().toString().equalsIgnoreCase(urlParams[0]))
                    throw new RestEndpointException(UNAUTHORIZED, "UUID Mismatch; Cookie Invalid");

                if (body.has("new_password")) {
                    Ref<EMailAddress> email = account.email;

                    if (!body.has("current_password"))
                        throw new RestEndpointException(BAD_REQUEST, "Old Password missing");
                    if (!body.use("current_password")
                            .map(UniNode::asString)
                            .accumulate(email, (pw, mail) -> account.tryLogin(mail, pw)))
                        throw new RestEndpointException(UNAUTHORIZED, "Old Password wrong");

                    body.use("new_password")
                            .map(UniNode::asString)
                            .combine(email, (pw, mail) -> UserAccount.encrypt(mail.toString(), pw))
                            .consume(account::putHash);
                } else account.updateFrom(body.asObjectNode());

                return AuthServerEndpoint.forwardToWidgetOr(headers, new REST.Header.List(), "../", "account");
            } catch (RestEndpointException ex) {
                if (ex.getStatusCode() == UNAUTHORIZED)
                    throw ex;
                throw new RestEndpointException(UNAUTHORIZED, "Underlying Message: " + ex.getMessage(), ex);
            }
        }
    },
    MODIFY_ACCOUNT_DATA_STORAGE(org.comroid.auth.rest.AuthEndpoint.MODIFY_ACCOUNT_DATA_STORAGE) {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UserDataStorage dataStorage = obtainDataStorage(context, headers, UUID.fromString(urlParams[0]), UUID.fromString(urlParams[1]));
            UniNode storageData = dataStorage.getData(urlParams[1]);
            return new REST.Response(OK, storageData);
        }

        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UserDataStorage dataStorage = obtainDataStorage(context, headers, UUID.fromString(urlParams[0]), UUID.fromString(urlParams[1]));
            UniNode storageData = dataStorage.putData(urlParams[1], body);
            try {
                dataStorage.storeData();
            } catch (IOException e) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not write data");
            }
            return new REST.Response(OK, storageData);
        }

        private UserDataStorage obtainDataStorage(Context context, REST.Header.List headers, UUID clientId, UUID serviceId) {
            ClientProvider clientProvider = context.requireFromContext(ClientProvider.class);
            OAuthAuthorization.AccessToken accessToken = clientProvider.findAccessToken(headers);

            if (!accessToken.getAuthorization().getResource().getUUID().equals(serviceId))
                throw new RestEndpointException(UNAUTHORIZED, "Invalid Token for Resource " + serviceId);
            if (!accessToken.getScopes().contains("storage"))
                throw new RestEndpointException(UNAUTHORIZED, "Missing scope: storage");

            UserAccount account = clientProvider.findClient(clientId).into(UserAccount.class);
            if (account == null)
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Internal Server Error: Invalid Client type");

            return account.getDataStorage(serviceId);
        }
    },
    REGISTRATION("/api/register") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                String email = body.use(EMAIL)
                        .map(UniNode::asString)
                        .map(str -> str.replace("%40", "@"))
                        .requireNonNull("No Email provided");
                String password = body.use("password")
                        .map(UniNode::asString)
                        .requireNonNull("No Password provided");

                UserAccount account = AuthServer.instance.getUserManager().createAccount(email, password);

                return AuthServerEndpoint.forwardToWidgetOr(headers, new REST.Header.List(), "../", "account");
            } catch (Throwable t) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not create user account", t);
            }
        }
    },
    LOGIN("/api/login") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                EMailAddress email = body.use(EMAIL)
                        .map(UniNode::asString)
                        .map(EMailAddress::parse)
                        .requireNonNull("No Email provided");
                String password = body.use("password")
                        .map(UniNode::asString)
                        .requireNonNull("No Password provided");

                UserSession session = AuthServer.instance.getUserManager().loginUser(email, password);

                REST.Header.List resp = new REST.Header.List();
                resp.add("Set-Cookie", session.getCookie());
                return forwardToWidgetOr(headers, resp, "../", "account");
            } catch (Throwable t) {
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not log in", t);
            }
        }

        @Override
        public REST.Response executeDELETE(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return new REST.Response(Polyfill.uri("logout"));
        }
    },
    LOGOUT("/logout") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            try {
                UserSession session = UserSession.findSession(headers);
                AuthServer.instance.getUserManager().closeSession(session);
                REST.Header.List response = new REST.Header.List();
                response.add(CommonHeaderNames.CACHE_CONTROL, "no-cache");
                response.add("Set-Cookie", UserSession.NULL_COOKIE);
                return forwardToWidgetOr(headers, response, "", "home");
            } catch (Throwable ignored) {
                return new REST.Response(Polyfill.uri("home"));
            }
        }
    },
    SERVICES(AuthEndpoint.SERVICES) {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            AuthServer server = context.requireFromContext(AuthServer.class);
            if (!server.getUserManager()
                    .findAccessToken(headers)
                    .getScopes()
                    .contains("admin"))
                throw new RestEndpointException(UNAUTHORIZED, "Missing Permit: admin");
            UniArrayNode array = context.createArrayNode();
            server.getServiceManager()
                    .getServices()
                    .map(service -> {
                        UniObjectNode data = service.toUniNode();
                        data.put("secret", service.getSecret());
                        return data;
                    }).forEach(array::add);
            return new REST.Response(OK, array);
        }
    },
    SERVICE_API(org.comroid.auth.rest.AuthEndpoint.SERVICE_API) {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UUID uuid;
            try {
                uuid = UUID.fromString(urlParams[0]);
            } catch (Exception e) {
                throw new RestEndpointException(BAD_REQUEST, "Malformed ID: " + urlParams[0], e);
            }
            // validate permission
            UserSession.findSession(headers).checkPermits(Permit.DEV);

            // get service
            Service service = AuthServer.instance.getServiceManager()
                    .getResource(uuid)
                    .orElseThrow(() -> new RestEndpointException(NOT_FOUND, "Service with ID " + uuid + " not found"));

            return new REST.Response(OK, service);
        }

        @Override
        public boolean allowMemberAccess() {
            return true;
        }

        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            final AuthServer server = context.requireFromContext(AuthServer.class);
            final ServiceManager serviceManager = server.getServiceManager();
            UUID uuid = urlParams[0].equals("00000000-0000-0000-0000-000000000000") ? null : UUID.fromString(urlParams[0]);

            UserSession.findSession(headers).checkPermits(Permit.ADMIN);

            context.getLogger().debug("POSTing Service with ID {} and body {}", uuid, body);
            UniObjectNode data = body.asObjectNode();
            if (!FileBasedService.Type.isValidData(data))
                if (uuid == null) {
                    uuid = UUID.randomUUID();
                    data.put(Service.ID, uuid.toString());
                } else// throw if data is not valid
                    throw new RestEndpointException(BAD_REQUEST, "Service Data is invalid");

            // check if service exists
            Service service;
            if (serviceManager.hasResource(uuid)) {
                // update existing service
                service = serviceManager.getResource(uuid).assertion();
                service.updateFrom(data);
                context.getLogger().info("Service {} data was updated: {}", service, service.toUniNode());
            } else {
                // create service
                service = serviceManager.createService(data);
                context.getLogger().info("Service {} was created", service);
            }
            return forwardToWidgetOr(headers, new REST.Header.List(), "../../", "service/" + uuid);
        }
    },
    DISCOVERY_OAUTH("/.well-known/oauth-authorization-server") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return discoveryResponse();
        }
    },
    DISCOVERY_OPENID("/.well-known/openid-configuration") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return discoveryResponse();
        }
    };

    public static final StreamSupplier<ServerEndpoint> values = StreamSupplier.of(values());
    private final String extension;
    private final String[] regex;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return AuthServer.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regex;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    AuthServerEndpoint(org.comroid.auth.rest.AuthEndpoint endpoint) {
        this(endpoint.getUrlExtension(), endpoint.getRegExpGroups());
    }

    AuthServerEndpoint(String extension, @Language("RegExp") String... regex) {
        this.extension = extension;
        this.regex = regex;
        this.pattern = buildUrlPattern();
    }

    public static REST.Response discoveryResponse() {
        return new REST.Response(OK, MimeType.JSON, new InputStreamReader(FrameBuilder.getResource("oauth-discovery.json")));
    }

    @NotNull
    private static REST.Response forwardToWidgetOr(REST.Header.List headers, REST.Header.List response, String prefix, String other) {
        String referrer = headers.getFirst(CommonHeaderNames.REFERER);
        referrer = referrer == null ? "" : referrer.substring(referrer.lastIndexOf('/') + 1);
        boolean isWidget = referrer.equals("widget");
        response.add(CommonHeaderNames.REDIRECT_TARGET, prefix + (isWidget ? "widget" : other));
        return new REST.Response(MOVED_PERMANENTLY, response);
    }
}
