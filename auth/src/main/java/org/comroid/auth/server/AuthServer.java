package org.comroid.auth.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.UncheckedCloseable;
import org.comroid.api.os.OS;
import org.comroid.auth.service.ServiceManager;
import org.comroid.auth.user.Permit;
import org.comroid.auth.user.UserManager;
import org.comroid.auth.user.UserSession;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.oauth.OAuth;
import org.comroid.oauth.rest.OAuthEndpoint;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.restless.adapter.java.JavaHttpAdapter;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service;
import org.comroid.uniform.Context;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;
import org.comroid.webkit.config.WebkitConfiguration;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.server.WebkitServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AuthServer implements ContextualProvider.Underlying, UncheckedCloseable, PagePropertiesProvider {
    //http://localhost:42000
    public static final Logger logger = LogManager.getLogger();
    public static final ContextualProvider MASTER_CONTEXT;
    public static final String UUID_PATTERN = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b";
    public static final String URL_BASE = "https://auth.comroid.org";
    public static final int PORT = 42000;
    public static final int SOCKET_PORT = 42001;
    public static final FileHandle DIR = new FileHandle("/srv/auth/", true);
    public static final FileHandle STATUS_CRED = DIR.createSubFile("status.cred");
    public static final FileHandle DATA = DIR.createSubDir("data");
    public static final SerializationAdapter SERI_LIB;
    public static final HttpAdapter HTTP_LIB;
    public static AuthServer instance;

    static {
        DIR.mkdir();
        DATA.mkdir();
        SERI_LIB = FastJSONLib.fastJsonLib;
        HTTP_LIB = new JavaHttpAdapter();
        MASTER_CONTEXT = ContextualProvider.create(SERI_LIB, HTTP_LIB);
        OAuth.CONTEXT = MASTER_CONTEXT;
        WebkitConfiguration.initialize(MASTER_CONTEXT);
    }

    private final ScheduledExecutorService executor;
    private final StatusConnection status;
    private final ContextualProvider context;
    private final UserManager userManager;
    private final ServiceManager serviceManager;
    private final WebkitServer server;

    public UserManager getUserManager() {
        return userManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public RefContainer<?, AuthConnection> getActiveConnections() {
        return server.getActiveConnections().flatMap(AuthConnection.class);
    }

    public AuthServer(ScheduledExecutorService executor) {
        logger.info("Booting up");
        try {
            this.executor = executor;

            logger.debug("Registering shutdown Hook");
            Thread shutdownHook = new Thread(this::close);
            shutdownHook.setPriority(Thread.MAX_PRIORITY);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            if (OS.isUnix) {
                logger.debug("Initializing Status Connection...");
                this.status = new StatusConnection(MASTER_CONTEXT, "auth-server", STATUS_CRED.getContent(true), executor);
            } else this.status = null;
            this.context = MASTER_CONTEXT.plus("Auth Server", this, executor);

            logger.debug("Starting UserManager");
            this.userManager = new UserManager(this);
            context.addToContext(userManager);

            logger.debug("Starting ServiceManager");
            this.serviceManager = new ServiceManager(this);
            context.addToContext(serviceManager);

            logger.debug("Starting Webkit server");
            this.server = new WebkitServer(
                    context.upgrade(Context.class),
                    this.executor,
                    URL_BASE,
                    OS.isWindows
                            ? InetAddress.getLoopbackAddress()
                            : InetAddress.getLocalHost(),
                    PORT,
                    SOCKET_PORT,
                    AuthConnection::new,
                    this,
                    AuthEndpoint.values.append(OAuthEndpoint.values)
            );
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            throw new RuntimeException("Could not start Auth Server", e);
        }

        if (status != null && !status.isPolling() && !status.startPolling())
            throw new UnsupportedOperationException("Could not start polling server status");
        logger.info("Ready!");
    }

    public static void main(String[] args) {
        instance = new AuthServer(Executors.newScheduledThreadPool(8));
    }

    @Override
    public Map<String, Object> findPageProperties(REST.Header.List headers) {
        Map<String, Object> map;
        try {
            UserSession session = UserSession.findSession(headers);
            map = session.connection.<Map<String, Object>>map(conn -> conn.properties)
                    .or(() -> {
                        Map<String, Object> o = new HashMap<>();
                        o.put("isValidSession", true);
                        o.put("sessionData", session.getSessionData());
                        return o;
                    }).assertion("internal error");

            if (session.hasPermits(Permit.ADMIN)) {
                Map<String, Object> adminData = new HashMap<>();
                Map<String, Object> services = new HashMap<>();
                serviceManager.getServices().forEach(service ->
                        services.put(service.getUUID().toString(), service.toUniNode()));
                adminData.put("service", services);
                map.put("adminData", adminData);
            }
        } catch (RestEndpointException unauthorized) {
            map = new HashMap<>();
            map.put("isValidSession", false);
            map.put("sessionData", null);
        }

        return map;
    }

    @Override
    public void close() {
        logger.info("Shutting down");
        try {
            status.stopPolling(Service.Status.OFFLINE)
                    .exceptionally(Polyfill.exceptionLogger(logger, "Could not stop Polling"));
            server.close();
        } catch (Throwable t) {
            logger.error("Could not shutdown Rest Server gracefully", t);
        }
        try {
            userManager.close();
        } catch (Throwable t) {
            logger.error("Could not shutdown UserManager gracefully", t);
        }
        logger.info("Goodbye!");
    }

    public static final class WebResources {
        private final ClassLoader classLoader;

        public InputStreamReader getAPI() {
            return getWebResourceByName("api.js");
        }

        private WebResources(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public InputStreamReader getPage(String name) {
            return getWebResourceByName(name + ".html");
        }

        public InputStreamReader getPanel(String name) {
            return getWebResourceByName(String.format("panel/%s.html", name));
        }

        public InputStreamReader getWebResourceByName(String name) {
            return new InputStreamReader(getResourceByName("html/" + name));
        }

        public InputStream getResourceByName(String name) {
            InputStream resource = classLoader.getResourceAsStream(name);
            if (resource == null)
                throw new NullPointerException("Could not locate resource: " + name);
            return resource;
        }
    }
}
