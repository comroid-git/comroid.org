package org.comroid.status;

import org.comroid.listnr.AbstractEventManager;
import org.comroid.listnr.ListnrCore;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.restless.socket.event.WebSocketPayload;
import org.comroid.status.entity.Service;
import org.comroid.status.event.GatewayEvent;
import org.comroid.status.event.GatewayPayload;
import org.comroid.status.gateway.Gateway;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.cache.ProvidedCache;
import org.comroid.uniform.node.UniObjectNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public final class StatusConnection
        extends AbstractEventManager<WebSocketPayload.Data, GatewayEvent<GatewayPayload>, GatewayPayload>
        implements DependenyObject {
    private final String serviceName;
    private final String token;
    private final Executor executor;
    private final REST<DependenyObject> rest;
    private final ProvidedCache<String, Service> serviceCache;
    private final Gateway gateway;
    private final Service ownService;

    public String getServiceName() {
        return serviceName;
    }

    public String getToken() {
        return token;
    }

    public Gateway getGateway() {
        return gateway;
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

    public StatusConnection(String serviceName, String token) {
        this(serviceName, token, Executors.newSingleThreadExecutor());
    }

    public StatusConnection(String serviceName, String token, Executor executor) {
        super(new ListnrCore(executor));

        this.serviceName = serviceName;
        this.token = token;
        this.executor = executor;
        this.rest = new REST<>(Adapters.HTTP_ADAPTER, Adapters.SERIALIZATION_ADAPTER, executor, this);
        this.serviceCache = new ProvidedCache<>(250, ForkJoinPool.commonPool(), this::requestServiceByName);
        this.gateway = new Gateway(this, Adapters.HTTP_ADAPTER, Adapters.SERIALIZATION_ADAPTER, this.executor);
        this.ownService = requestServiceByName(serviceName).join();
    }

    public CompletableFuture<Service> updateStatus(Service.Status status) {
        final UniObjectNode data = rest.getSerializationAdapter().createUniObjectNode();

        data.put("status", ValueType.INTEGER, status.getValue());

        return rest.request(Service.class)
                .method(REST.Method.POST)
                .endpoint(Endpoint.UPDATE_SERVICE_STATUS.complete(serviceName))
                .addHeader(CommonHeaderNames.AUTHORIZATION, token)
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
