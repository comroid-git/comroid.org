package org.comroid.status.entity;

import org.comroid.api.IntEnum;
import org.comroid.api.Polyfill;
import org.comroid.api.WrappedFormattable;
import org.comroid.restless.REST;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.status.StatusConnection;
import org.comroid.status.rest.Endpoint;
import org.comroid.util.StandardValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Location(Service.Bind.class)
public interface Service extends Entity, WrappedFormattable {
    @Language("RegExp")
    String NAME_REGEX = "\\w[\\w\\d-]+";

    default String getDisplayName() {
        return requireNonNull(Bind.DisplayName);
    }

    default Status getStatus() {
        return requireNonNull(Bind.Status);
    }

    default Optional<URL> getURL() {
        return wrap(Bind.URL);
    }

    @Override
    default String getDefaultFormattedName() {
        return getDisplayName();
    }

    @Override
    default String getAlternateFormattedName() {
        return getName();
    }

    CompletableFuture<Status> requestStatus();

    CompletableFuture<Service> updateStatus(Status status);

    enum Status implements IntEnum {
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

    interface Bind extends Entity.Bind {
        @RootBind
        GroupBind<Service> Root
                = Entity.Bind.Root.subGroup("service",
                (connection, node) -> new Basic(connection.requireFromContext(StatusConnection.class), node.asObjectNode()));
        VarBind<Service, String, String, String> DisplayName
                = Root.createBind("display_name")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .setRequired(true)
                .build();
        VarBind<Service, Integer, Service.Status, Service.Status> Status
                = Root.createBind("status")
                .extractAs(StandardValueType.INTEGER)
                .andRemap(Service.Status::valueOf)
                .onceEach()
                .setRequired(true)
                .build();
        VarBind<Service, String, URL, URL> URL
                = Root.createBind("url")
                .extractAs(StandardValueType.STRING)
                .andRemap(Polyfill::url)
                .onceEach()
                .setRequired(false)
                .build();
    }

    final class Basic extends DataContainerBase<Entity> implements Service {
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
                        put(Service.Bind.Status, status.value);
                        return status;
                    });
        }

        @Override
        public CompletableFuture<Service> updateStatus(Status status) {
            if (getStatus() == status)
                return CompletableFuture.completedFuture(this);
            if (!Objects.equals(put(Service.Bind.Status, status), status.getValue()))
                return connection.getRest()
                    .request(Service.Bind.Root)
                    .method(REST.Method.POST)
                    .endpoint(Endpoint.UPDATE_SERVICE_STATUS.complete(getName()))
                    .buildBody(BodyBuilderType.OBJECT, obj -> obj.put(Service.Bind.Status, status))
                    .execute$deserializeSingle();
            else return Polyfill.failedFuture(new RuntimeException("Unable to change status"));
        }
    }
}
