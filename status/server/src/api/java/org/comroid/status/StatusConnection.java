package org.comroid.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.status.entity.Service;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class StatusConnection implements ContextualProvider.Underlying {
    public static final String SERVER = "https://api.status.comroid.org";
    public static final Logger Logger = LogManager.getLogger("StatusConnection");
    public static ContextualProvider CONTEXT;
    private final Logger logger;
    @Nullable
    private final String serviceName;
    private final Service service;
    private final String token;
    private final ScheduledExecutorService executor;
    public int rate = 60; // seconds
    private boolean polling = false;

    public String getServiceName() {
        return serviceName;
    }

    public String getToken() {
        return token;
    }

    public Service getService() {
        return service;
    }

    public boolean isPolling() {
        return polling;
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return CONTEXT;
    }

    public StatusConnection(String serviceName, String token) {
        this(serviceName, token, Executors.newScheduledThreadPool(4));
    }

    public StatusConnection(String serviceName, String token, ScheduledExecutorService executor) {
        this.logger = serviceName == null ? Logger : LogManager.getLogger(String.format("StatusConnection(%s)", serviceName));
        this.service = requestServiceByName(serviceName).join();
        this.serviceName = serviceName;
        this.token = token;
        this.executor = executor;
    }

    @Override
    public String toString() {
        return String.format("StatusConnection{serviceName='%s', service=%s}", serviceName, "null");//, ownService == null ? "undefined" : ownService.get());
    }

    public boolean startPolling() {
        if (polling)
            return false;
        executor.schedule(() -> {
            try {
                sendPoll();
                startPolling();
            } catch (Throwable t) {
                logger.error("Error while executing Poll", t);
            }
        }, rate, TimeUnit.SECONDS);
        return (polling = true);
    }

    public CompletableFuture<?> stopPolling() {
        if (!polling)
            return Polyfill.failedFuture(new RuntimeException("Connection is not polling!"));
        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        return CompletableFuture.supplyAsync(() -> new RestTemplate().exchange(
                        String.format("%s/service/%s/poll", SERVER, serviceName),
                        HttpMethod.DELETE,
                        new HttpEntity<>(rate, createHeaders("Authorization", token)),
                        Service.class))
                .thenApply(ResponseEntity::getBody);
    }

    private CompletableFuture<?> sendPoll() {
        logger.debug("Sending Poll");

        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        return CompletableFuture.supplyAsync(() -> new RestTemplate().exchange(
                        String.format("%s/service/%s/poll", SERVER, serviceName),
                        HttpMethod.POST,
                        new HttpEntity<>(rate, createHeaders("Authorization", token)),
                        Service.class))
                .thenApply(ResponseEntity::getBody);
    }

    public CompletableFuture<Service> updateStatus(Service.Status status) {
        if (serviceName == null)
            throw new NoSuchElementException("No service name defined");
        return CompletableFuture.supplyAsync(() -> new RestTemplate().exchange(
                String.format("%s/service/%s/status", SERVER, serviceName),
                        HttpMethod.POST,
                        new HttpEntity<>(status, createHeaders("Authorization", token)),
                        Service.class))
                .thenApply(ResponseEntity::getBody);
    }

    public CompletableFuture<Collection<Service>> requestServices() {
        return CompletableFuture.supplyAsync(() -> new RestTemplate().exchange(
                        String.format("%s/services", SERVER),
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        new ParameterizedTypeReference<List<Service>>(){}))
                .thenApply(ResponseEntity::getBody);
    }

    public CompletableFuture<Service> requestServiceByName(String name) {
        return CompletableFuture.supplyAsync(() -> new RestTemplate().exchange(
                        String.format("%s/service/%s", SERVER, serviceName),
                        HttpMethod.GET,
                        HttpEntity.EMPTY,
                        Service.class))
                .thenApply(ResponseEntity::getBody);
    }

    private HttpHeaders createHeaders(String... keyValuePairs) {
        HttpHeaders headers = new HttpHeaders();
        for (int i = 0; i < keyValuePairs.length; i++)
            headers.add(keyValuePairs[i], keyValuePairs[++i]);
        return headers;
    }
}
