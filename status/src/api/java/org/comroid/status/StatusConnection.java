package org.comroid.status;

import org.comroid.mutatio.span.Span;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.cache.ProvidedCache;
import org.comroid.uniform.node.UniObjectNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class StatusConnection implements DependenyObject {
    private final String serviceName;
    private final String token;
    private final REST<DependenyObject> rest;
    private final ProvidedCache<String, Service> serviceCache;
    private final Service ownService;

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

    public StatusConnection(String serviceName, String token) {
        this.serviceName = serviceName;
        this.token = token;
        this.rest = new REST<>(Adapters.HTTP_ADAPTER, Adapters.SERIALIZATION_ADAPTER, this);
        this.serviceCache = new ProvidedCache<>(250, ForkJoinPool.commonPool(), this::requestServiceByName);
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
