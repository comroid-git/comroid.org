package org.comroid.auth.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.os.OS;
import org.comroid.common.io.FileHandle;
import org.comroid.restless.adapter.java.JavaHttpAdapter;
import org.comroid.restless.server.RestServer;
import org.comroid.status.StatusConnection;
import org.comroid.uniform.adapter.json.fastjson.FastJSONLib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AuthServer implements ContextualProvider.Underlying {
    public static final Logger logger = LogManager.getLogger("AuthServer");
    public static final ContextualProvider MASTER_CONTEXT;
    public static final String URL_BASE = "https://auth.comroid.org/";
    public static final int PORT = 42020;
    public static final FileHandle DIR = new FileHandle("/srv/auth/", true);
    public static final FileHandle STATUS_CRED = DIR.createSubFile("status.cred");
    public static final FileHandle DATA = DIR.createSubDir("data");
    public static final FileHandle CACHE_FILE = DATA.createSubFile("cache.json");
    public static AuthServer instance;

    static {
        MASTER_CONTEXT = ContextualProvider.create(FastJSONLib.fastJsonLib, new JavaHttpAdapter());
    }

    private final ScheduledExecutorService executor;
    private final StatusConnection status;
    private final ContextualProvider context;
    private final RestServer rest;

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public AuthServer(ScheduledExecutorService executor) {
        logger.info("Booting up");
        try {
            this.executor = executor;
            logger.debug("Initializing Status Connection...");
            this.status = new StatusConnection(MASTER_CONTEXT, "auth-server", STATUS_CRED.getContent(true), executor);
            this.context = MASTER_CONTEXT.plus("NetBoxServer", executor);
            logger.debug("Starting RestServer with {} endpoints", Endpoint.values().length);
            this.rest = new RestServer(MASTER_CONTEXT, this.executor, URL_BASE, InetAddress.getLocalHost(), PORT, Endpoint.values());
        } catch (UnknownHostException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            throw new RuntimeException("Could not start NetBox Server", e);
        }

        if (OS.current == OS.UNIX && !status.isPolling() && !status.startPolling())
            throw new UnsupportedOperationException("Could not start polling server status");
        logger.info("Ready!");
    }

    public static void main(String[] args) {
        instance = new AuthServer(Executors.newScheduledThreadPool(8));
    }
}
