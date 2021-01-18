package org.comroid.status;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.common.io.FileHandle;
import org.comroid.common.jvm.JITAssistant;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.REST;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.impl.StandardValueType;
import org.comroid.uniform.cache.ProvidedCache;
import org.comroid.uniform.node.UniObjectNode;

import java.util.concurrent.*;

import static org.comroid.restless.CommonHeaderNames.AUTHORIZATION;

public final class StatusConnection implements ContextualProvider.Underlying {
    private final AdapterDefinition adapterDefinition;
    private final String serviceName;
    private final String token;
    private final ScheduledExecutorService executor;
    private final REST rest;
    private final ProvidedCache<String, Service> serviceCache;
    private final Service ownService;
    public int refreshTimeout = 60; // seconds
    public int crashedTimeout = 3600; // seconds
    private boolean polling = false;

    public String getServiceName() {
        return serviceName;
    }

    public String getToken() {
        return token;
    }

    public Service getService() {
        return ownService;
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
        return adapterDefinition;
    }

    public StatusConnection(AdapterDefinition context, String serviceName, FileHandle tokenFile) {
        this(context, serviceName, tokenFile.getContent(), Executors.newScheduledThreadPool(4));
    }

    public StatusConnection(AdapterDefinition context, String serviceName, String token, ScheduledExecutorService executor) {
        this.adapterDefinition = context;
        this.serviceName = serviceName;
        this.token = token;
        this.executor = executor;
        this.rest = new REST(adapterDefinition, executor);
        this.serviceCache = new ProvidedCache<>(context, 250, ForkJoinPool.commonPool(), this::requestServiceByName);
        this.ownService = requestServiceByName(serviceName).join();

        JITAssistant.prepareStatic(Service.class);
    }

    public boolean startPolling() {
        if (polling)
            return false;
        executePoll().join();
        return (polling = true);
    }

    public CompletableFuture<Service> stopPolling(Service.Status newStatus) {
        if (!polling)
            return Polyfill.failedFuture(new RuntimeException("Connection is not polling!"));
        return rest.request(Service.Bind.Root)
                .method(REST.Method.DELETE)
                .endpoint(Endpoint.POLL.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .buildBody(BodyBuilderType.OBJECT, obj ->
                        obj.put(Service.Bind.Status, newStatus.getValue()))
                .execute$autoCache(Service.Bind.Name, serviceCache)
                .thenApply(Span::requireSingle);
    }

    private CompletableFuture<Void> executePoll() {
        return sendPoll().thenRun(this::schedulePoll);
    }

    private void schedulePoll() {
        executor.schedule(this::executePoll, refreshTimeout, TimeUnit.SECONDS);
    }

    public CompletableFuture<Service> sendPoll() {
        return rest.request(Service.Bind.Root)
                .method(REST.Method.POST)
                .endpoint(Endpoint.POLL.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .buildBody(BodyBuilderType.OBJECT, obj -> {
                    obj.put("status", StandardValueType.INTEGER, Service.Status.ONLINE.getValue());
                    obj.put("expected", StandardValueType.INTEGER, refreshTimeout);
                    obj.put("timeout", StandardValueType.INTEGER, crashedTimeout);
                })
                .execute$autoCache(Service.Bind.Name, serviceCache)
                .thenApply(Span::requireNonNull);
    }

    public CompletableFuture<Service> updateStatus(Service.Status status) {
        final UniObjectNode data = rest.requireFromContext(SerializationAdapter.class)
                .createUniObjectNode();

        data.put("status", StandardValueType.INTEGER, status.getValue());

        return rest.request(Service.class)
                .method(REST.Method.POST)
                .endpoint(Endpoint.UPDATE_SERVICE_STATUS.complete(serviceName))
                .addHeader(AUTHORIZATION, token)
                .body(data.toString())
                .execute$autoCache(Service.Bind.Name, serviceCache)
                .thenApply(Span::requireNonNull);
    }

    public CompletableFuture<Service> requestServiceByName(String name) {
        return rest.request(Service.class)
                .method(REST.Method.GET)
                .endpoint(Endpoint.SPECIFIC_SERVICE.complete(name))
                .execute$autoCache(Service.Bind.Name, serviceCache)
                .thenApply(Span::requireNonNull);
    }
}
