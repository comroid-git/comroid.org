package org.comroid.status.entity;

import org.comroid.api.IntegerAttribute;
import org.jetbrains.annotations.ApiStatus;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Entity
@Table(name = "services")
public class Service {
    private static final RestTemplate SpecificService = new RestTemplate();
    private static final String EndpointSpecificService = "https://api.status.comroid.org/service/{}/status";
    @Id
    private String name;
    @Column
    private String displayName;
    @Column
    private URL url;
    @Column
    private String pingHost;
    @Column
    private Status status;

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Status getStatus() {
        return status;
    }

    @ApiStatus.Internal
    public void setStatus(Status status) {
        this.status = status;
    }

    public Optional<URL> getURL() {
        return Optional.ofNullable(url);
    }

    public CompletableFuture<Status> requestStatus() {
        return CompletableFuture.supplyAsync(() -> SpecificService.getForObject(EndpointSpecificService, Status.class, getName()))
                .thenApply(status -> this.status = status);
    }

    public CompletableFuture<Service> updateStatus(Status status) {
        return CompletableFuture.supplyAsync(() -> SpecificService.postForObject(EndpointSpecificService, status, Status.class, getName()))
                .thenApply(newStatus -> {
                    this.status = newStatus;
                    return this;
                });
    }

    public enum Status implements IntegerAttribute {
        UNKNOWN,

        OFFLINE,
        CRASHED,
        MAINTENANCE,

        NOT_RESPONDING,

        ONLINE;

        public static Status valueOf(int value) {
            return IntegerAttribute.valueOf(value, Status.class).assertion();
        }

        @Override
        public String toString() {
            return name();
        }
    }
}
