package org.comroid.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.RefList;
import org.comroid.mutatio.model.RefOPs;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.REST;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.ProvidedCache;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.comroid.restless.CommonHeaderNames.AUTHORIZATION;

public final class StatusConnection implements ContextualProvider.Underlying {
    public static ContextualProvider CONTEXT;
    public static final Logger Logger = LogManager.getLogger("StatusConnection");
    private final Logger logger;
    @Nullable
    private final String serviceName;
    private final String token;
    private final ScheduledExecutorService executor;
    private final REST rest;
    private final ProvidedCache<String, Service> serviceCache;
    private final Reference<Service> ownService;
    public int refreshTimeout = 60; // seconds
    public int crashedTimeout = 120; // seconds
    private boolean polling = false;

    public String getServiceName() {
        return serviceName;
    }

    public String getToken() {
        return token;
    }

    public Service getService() {
        return ownService.assertion();
    }

    public REST getRest() {
        return rest;
    }

    public ProvidedCache<String, Service> getServiceCache() {
        return serviceCache;
    }

    public boolean isPolling() {
        return polling;
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return CONTEXT;
    }

    public StatusConnection(ContextualProvider context, FileHandle tokenFile) {
        this(context, null, tokenFile);
    }

    public StatusConnection(ContextualProvider context, @Nullable String serviceName, FileHandle tokenFile) {
        this(context, serviceName, tokenFile.getContent(), Executors.newScheduledThreadPool(4));
    }

    public StatusConnection(ContextualProvider context, @Nullable String serviceName, String token, ScheduledExecutorService executor) {
        this.logger = serviceName == null ? Logger : LogManager.getLogger(String.format("StatusConnection(%s)", serviceName));
        this.serviceName = serviceName;
        this.token = token;
        this.executor = executor;
        logger.debug("Building Cache...");
        this.serviceCache = new ProvidedCache<>(context, 250, ForkJoinPool.commonPool(), this::requestServiceByName);
        CONTEXT.addToContext(executor, serviceCache);
        this.rest = new REST(CONTEXT, executor);
        CONTEXT.addToContext(rest);
        this.ownService = new FutureReference<>(requestServiceByName(serviceName)
                .exceptionally(exceptionLogger("Could not request own service")));
    }

    @Override
    public String toString() {
        return String.format("StatusConnection{serviceName='%s', service=%s}", serviceName, ownService == null ? "undefined" : ownService.get());
    }

    public boolean startPolling() {
        if (polling)
            return false;
        executor.scheduleAtFixedRate(() -> {
            try {
                sendPoll().exceptionally(exceptionLogger("Error ocurred during Poll"));
            } catch (Throwable t) {
                logger.error("Error while executing Poll", t);
            }
        }, 0, refreshTimeout, TimeUnit.SECONDS);
        return (polling = true);
    }

    public CompletableFuture<Service> stopPolling(Service.Status newStatus) {
        if (!polling)
            return Polyfill.failedFuture(new RuntimeException("Connection is not polling!"));
        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        return rest.request(Service.Type)
                .method(REST.Method.DELETE)
                .endpoint(Endpoint.POLL.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .buildBody(BodyBuilderType.OBJECT, obj -> obj.put(Service.STATUS, newStatus))
                .execute$autoCache(Service.NAME, serviceCache)
                .thenApply(services -> {
                    polling = false;
                    return services.getAny();
                });
    }

    private CompletableFuture<Service> sendPoll() {
        logger.debug("Sending Poll");

        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        return rest.request(Service.Type)
                .method(REST.Method.POST)
                .endpoint(Endpoint.POLL.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .buildBody(BodyBuilderType.OBJECT, obj -> {
                    obj.put(Service.STATUS, Service.Status.ONLINE);
                    obj.put("expected", refreshTimeout);
                    obj.put("timeout", crashedTimeout);
                })
                .execute$autoCache(Service.NAME, serviceCache)
                .thenApply(RefOPs::getAny);
    }

    public CompletableFuture<Service> updateStatus(Service.Status status) {
        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        final UniObjectNode data = rest.requireFromContext(SerializationAdapter.class)
                .createObjectNode();

        data.put(Service.STATUS, status);

        return rest.request(Service.class)
                .method(REST.Method.POST)
                .endpoint(Endpoint.UPDATE_SERVICE_STATUS.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .body(data.toString())
                .execute$autoCache(Service.NAME, serviceCache)
                .thenApply(RefOPs::getAny);
    }

    public CompletableFuture<Service> requestServiceByName(String name) {
        return rest.request(Service.class)
                .method(REST.Method.GET)
                .endpoint(Endpoint.SPECIFIC_SERVICE.complete(name))
                .execute$autoCache(Service.NAME, serviceCache)
                .thenApply(RefOPs::getAny);
    }

    private <T> Function<Throwable, T> exceptionLogger(String message) {
        return t -> {
            logger.error(message, t);
            return null;
        };
    }
}
