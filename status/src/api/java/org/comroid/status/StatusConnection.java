package org.comroid.status;

import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.REST;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.cache.ProvidedCache;
import org.comroid.uniform.node.UniObjectNode;

import java.util.concurrent.*;

import static org.comroid.restless.CommonHeaderNames.AUTHORIZATION;

public final class StatusConnection implements DependenyObject {
    private final String serviceName;
    private final String token;
    private final ScheduledExecutorService executor;
    private final REST<DependenyObject> rest;
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

    public REST<DependenyObject> getRest() {
        return rest;
    }

    public ProvidedCache<String, Service> getServiceCache() {
        return serviceCache;
    }

    public StatusConnection(String serviceName, FileHandle tokenFile) {
        this(serviceName, tokenFile.getContent(), Executors.newScheduledThreadPool(4));
    }

    public StatusConnection(String serviceName, String token, ScheduledExecutorService executor) {
        this.serviceName = serviceName;
        this.token = token;
        this.executor = executor;
        this.rest = new REST<>(Adapters.HTTP_ADAPTER, Adapters.SERIALIZATION_ADAPTER, this, executor);
        this.serviceCache = new ProvidedCache<>(250, ForkJoinPool.commonPool(), this::requestServiceByName);
        this.ownService = requestServiceByName(serviceName).join();
    }

    public boolean startPolling() {
        if (polling)
            return false;
        executePoll();
        return (polling = true);
    }
    }

    private void executePoll() {
        sendPoll().thenRun(this::schedulePoll);
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
                    obj.put("status", ValueType.INTEGER, Service.Status.ONLINE.getValue());
                    obj.put("expected", ValueType.INTEGER, refreshTimeout);
                    obj.put("timeout", ValueType.INTEGER, crashedTimeout);
                })
                .execute$autoCache(Service.Bind.Name, serviceCache)
                .thenApply(Span::requireNonNull);
    }

    public CompletableFuture<Service> updateStatus(Service.Status status) {
        final UniObjectNode data = rest.getSerializationAdapter().createUniObjectNode();

        data.put("status", ValueType.INTEGER, status.getValue());

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
