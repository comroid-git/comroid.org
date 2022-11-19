package org.comroid.status.entity;

import org.comroid.api.IntegerAttribute;
import org.comroid.api.Polyfill;
import org.comroid.api.WrappedFormattable;
import org.comroid.status.StatusConnection;
import org.comroid.util.StandardValueType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface Service extends Entity, WrappedFormattable {
    @Language("RegExp")
    String NAME_REGEX = "\\w[\\w\\d-]+";

    String getDisplayName();

    Status getStatus();

    Optional<URL> getURL();

    @Override
    default String getPrimaryName() {
        return getDisplayName();
    }

    @Override
    default String getAlternateName() {
        return getName();
    }

    CompletableFuture<Status> requestStatus();

    CompletableFuture<Service> updateStatus(Status status);

    enum Status implements IntegerAttribute {
        UNKNOWN(0),

        OFFLINE(1),
        CRASHED(2),
        MAINTENANCE(3),

        NOT_RESPONDING(4),

        ONLINE(5);

        private final int value;

        @Override
        public @NotNull Integer getValue() {
            return value;
        }

        Status(int value) {
            this.value = value;
        }

        public static Status valueOf(int value) {
            return Arrays.stream(values())
                    .filter(it -> it.value == value)
                    .findAny()
                    .orElse(UNKNOWN);
        }

        @Override
        public String toString() {
            return name();
        }
    }

    final class Basic implements Service {
        private final StatusConnection connection;

        public Basic(StatusConnection connection, UniObjectNode node) {
            super(connection, node);

            this.connection = connection;
        }

        @Override
        public CompletableFuture<Status> requestStatus() {
            return connection.getRest()
                    .request()
                    .method(REST.Method.GET)
                    .endpoint(Endpoint.SPECIFIC_SERVICE.complete(getName()))
                    .execute$deserializeSingle()
                    .thenApply(node -> node.get("status").asInt())
                    .thenApply(Status::valueOf)
                    .thenApply(status -> {
                        put(STATUS, status.value);
                        return status;
                    });
        }

        @Override
        public CompletableFuture<Service> updateStatus(Status status) {
            if (getStatus() == status)
                return CompletableFuture.completedFuture(this);
            if (!Objects.equals(put(STATUS, status), status.getValue()))
                return connection.getRest()
                        .request(Service.Type)
                        .method(REST.Method.POST)
                        .endpoint(Endpoint.UPDATE_SERVICE_STATUS.complete(getName()))
                        .buildBody(BodyBuilderType.OBJECT, obj -> obj.put(STATUS, status))
                        .execute$deserializeSingle();
            else return Polyfill.failedFuture(new RuntimeException("Unable to change status"));
        }
    }
}
