package org.comroid.status;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.comroid.common.iter.Span;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.status.entity.Entity;
import org.comroid.status.entity.Service;
import org.comroid.status.rest.Endpoint;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;

public enum StatusUpdater {
    instance;

    private final CompletableFuture<HttpAdapter> httpAdapterFuture          = new CompletableFuture<>();
    private final CompletableFuture<SerializationAdapter> seriAdapterFuture = new CompletableFuture<>();
    private final CompletableFuture<REST<StatusUpdater>> restFuture         = CompletableFuture.allOf(httpAdapterFuture,
            seriAdapterFuture
    )
            .thenApplyAsync(nil -> new REST<>(httpAdapterFuture.join(), seriAdapterFuture.join()));
    private final CompletableFuture<Container> containerFuture              = restFuture.thenApplyAsync(Container::new)
            .thenComposeAsync(Container::initialize);

    public final CompletableFuture<?> initialize(HttpAdapter httpAdapter, SerializationAdapter seriLib) {
        if (httpAdapterFuture.isDone() | seriAdapterFuture.isDone()) {
            throw new IllegalStateException("Adapters already initialized!");
        }

        httpAdapterFuture.complete(Objects.requireNonNull(httpAdapter));
        seriAdapterFuture.complete(Objects.requireNonNull(seriLib));

        return containerFuture;
    }

    public final CompletableFuture<HttpAdapter> getHttpAdapter() {
        return httpAdapterFuture;
    }

    public final CompletableFuture<SerializationAdapter> getSerializationAdapter() {
        return seriAdapterFuture;
    }

    private class Container {
        private final REST<StatusUpdater> restClient;
        private final Cache<UUID, Entity> cache;

        private Container(REST<StatusUpdater> restClient) {
            this.restClient = restClient;
            this.cache      = new BasicCache<>();
        }

        private CompletableFuture<Span<Service>> initialize() {
            return restClient.request(Service.class)
                    .method(REST.Method.GET)
                    .url(Endpoint.LIST_SERVICES.create())
                    .execute$autoCache(Entity.Bind.ID, cache);
        }
    }
}
