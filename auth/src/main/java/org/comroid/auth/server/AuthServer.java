package org.comroid.auth.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.UncheckedCloseable;
import org.comroid.api.os.OS;
import org.comroid.auth.user.UserManager;
import org.comroid.common.io.FileHandle;
import org.comroid.restless.adapter.java.JavaHttpAdapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.StatusConnection;
import org.comroid.status.entity.Service;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public final class AuthServer implements ContextualProvider.Underlying, UncheckedCloseable {
    //http://localhost:42000
    public static final Logger logger = LogManager.getLogger("AuthServer");
    public static final ContextualProvider MASTER_CONTEXT;
    public static final String URL_BASE = "https://auth.comroid.org/";
    public static final int PORT = 42000;
    public static final String[] WEB_RESOURCES = new String[]{"api.js", "login.html", "register.html", "account.html", "widget.html"};
    public static final FileHandle DIR = new FileHandle("/srv/auth/", true);
    public static final FileHandle STATUS_CRED = DIR.createSubFile("status.cred");
    public static final FileHandle DATA = DIR.createSubDir("data");
    public static final FileHandle WEB = DIR.createSubDir("web");
    public static AuthServer instance;

    static {
        DIR.mkdir();
        DATA.mkdir();
        WEB.mkdir();
        MASTER_CONTEXT = ContextualProvider.create(FastJSONLib.fastJsonLib, new JavaHttpAdapter());
    }

    private final ScheduledExecutorService executor;
    private final StatusConnection status;
    private final ContextualProvider context;
    private final UserManager userManager;
    private final RestServer rest;

    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public AuthServer(ScheduledExecutorService executor) {
        logger.info("Booting up");
        try {
            this.executor = executor;

            logger.debug("Registering shutdown Hook");
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));

            if (OS.current == OS.UNIX) {
                logger.debug("Initializing Status Connection...");
                this.status = new StatusConnection(MASTER_CONTEXT, "auth-server", STATUS_CRED.getContent(true), executor);
            } else this.status = null;
            this.context = MASTER_CONTEXT.plus("Auth Server", executor);

            logger.debug("Starting UserManager");
            this.userManager = new UserManager(this);
            context.addToContext(userManager);

            logger.debug("Starting Rest server");
            this.rest = new RestServer(context, this.executor, URL_BASE, OS.current == OS.WINDOWS ? InetAddress.getLoopbackAddress() : InetAddress.getLocalHost(), PORT, Endpoint.values());
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            throw new RuntimeException("Could not start Auth Server", e);
        }

        if (status != null && !status.isPolling() && !status.startPolling())
            throw new UnsupportedOperationException("Could not start polling server status");
        logger.info("Ready!");
    }

    private static void extractWebResources() {
        Stream.of(WEB_RESOURCES)
                .map(WEB::createSubFile)
                .forEach(file -> {
                    String resource = String.format("html/%#s", file);
                    try (
                            InputStream is = ClassLoader.getSystemResourceAsStream(resource);
                            OutputStream os = new FileOutputStream(file, false)
                    ) {
                        if (is == null)
                            throw new NoSuchElementException("No resource found with name " + resource);
                        is.transferTo(os);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void main(String[] args) {
        logger.debug("Extracting web resources");
        extractWebResources();

        instance = new AuthServer(Executors.newScheduledThreadPool(8));
    }

    @Override
    public void close() {
        logger.info("Shutting down");
        try {
            status.stopPolling(Service.Status.OFFLINE)
                    .exceptionally(Polyfill.exceptionLogger(logger, "Could not stop Polling"));
            rest.close();
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
}
