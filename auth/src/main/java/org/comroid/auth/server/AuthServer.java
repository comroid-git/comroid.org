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
import org.jetbrains.annotations.Nullable;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.AsyncResponse;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public final class AuthServer implements ContextualProvider.Underlying, UncheckedCloseable {
    //http://localhost:42000
    public static final Logger logger = LogManager.getLogger("AuthServer");
    public static final ContextualProvider MASTER_CONTEXT;
    public static final String URL_BASE = "https://auth.comroid.org/";
    public static final int PORT = 42000;
    public static final FileHandle DIR = new FileHandle("/srv/auth/", true);
    public static final FileHandle STATUS_CRED = DIR.createSubFile("status.cred");
    public static final FileHandle DATA = DIR.createSubDir("data");
    public static final WebResources Resources;
    public static AuthServer instance;

    static {
        DIR.mkdir();
        DATA.mkdir();
        Resources = new WebResources(ClassLoader.getSystemClassLoader());
        MASTER_CONTEXT = ContextualProvider.create(FastJSONLib.fastJsonLib, new JavaHttpAdapter());
    }

    private final ScheduledExecutorService executor;
    private final @Nullable StatusConnection status;
    private final ContextualProvider context;
    private final UserManager userManager;
    private final RestServer rest;
    private final @Nullable Mailer mailer;

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

            if (OS.current == OS.WINDOWS) {
                logger.debug("Loading Mailer");
                this.mailer = MailerBuilder.withTransportStrategy(TransportStrategy.SMTPS)
                        .withSMTPServer("mail.comroid.org", 465 /* todo: add authentication data */)
                        .buildMailer();
            } else mailer = null;
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

    public CompletableFuture<?> sendEmail(String recipient, Consumer<EmailPopulatingBuilder> emailBuilder) {
        if (mailer == null)
            return CompletableFuture.failedFuture(new RuntimeException("Mailer is not initialized"));
        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from("auth@comroid.org")
                .to(recipient);
        emailBuilder.accept(builder);

        final CompletableFuture<Void> future = new CompletableFuture<>();
        Email email = builder.buildEmail();
        AsyncResponse async = mailer.sendMail(email, true);
        if (async == null)
            throw new RuntimeException("Could not send E-Mail to " + recipient);
        async.onException(future::completeExceptionally);
        async.onSuccess(() -> future.complete(null));
        return future;
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
