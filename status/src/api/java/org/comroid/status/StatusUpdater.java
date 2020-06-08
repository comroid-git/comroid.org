package org.comroid.status;

import org.comroid.mutatio.span.Span;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public enum StatusUpdater implements DependenyObject {
    instance;

    private final CompletableFuture<HttpAdapter> httpAdapterFuture = new CompletableFuture<>();
    private final CompletableFuture<SerializationAdapter> seriAdapterFuture = new CompletableFuture<>();
    private final CompletableFuture<REST<DependenyObject>> restFuture = CompletableFuture.allOf(httpAdapterFuture,
            seriAdapterFuture
    )
            .thenApply(nil -> new REST<>(httpAdapterFuture.join(), seriAdapterFuture.join()));
    private final CompletableFuture<Container> containerFuture = restFuture.thenApply(Container::new)
            .thenCompose(Container::initialize);

    public final CompletableFuture<HttpAdapter> getHttpAdapter() {
        return httpAdapterFuture;
    }

    public final CompletableFuture<SerializationAdapter> getSerializationAdapter() {
        return seriAdapterFuture;
    }

    public final CompletableFuture<?> initialize(HttpAdapter httpAdapter, SerializationAdapter seriLib) {
        if (httpAdapterFuture.isDone() | seriAdapterFuture.isDone()) {
            throw new IllegalStateException("Adapters already initialized!");
        }

        httpAdapterFuture.complete(Objects.requireNonNull(httpAdapter));
        seriAdapterFuture.complete(Objects.requireNonNull(seriLib));

        return containerFuture;
    }

    public CompletableFuture<? extends Collection<Service>> requestAllServices() {
        return containerFuture.thenCompose(Container::requestAllServices);
    }

    class Container {
        private final REST<DependenyObject> restClient;
        private final Cache<String, Entity> cache;

        private Container(REST<DependenyObject> restClient) {
            this.restClient = restClient;
            this.cache = new BasicCache<>();
        }

        public CompletableFuture<Span<Service>> requestAllServices() {
            return restClient.request(Service.Bind.Root)
                    .method(REST.Method.GET)
                    .endpoint(Endpoint.LIST_SERVICES.complete())
                    .execute$autoCache(Entity.Bind.Name, cache);
        }

        private CompletableFuture<Container> initialize() {
            return CompletableFuture.allOf(requestAllServices())
                    .thenApply(nil -> this);
        }
    }
}
