package org.comroid.status;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.HttpAdapter;
import org.comroid.uniform.SerializationAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public interface DependenyObject {
    String URL_BASE = "https://api.status.comroid.org/";

    final class Adapters {
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull SerializationAdapter<?, ?, ?> SERIALIZATION_ADAPTER = null;
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull HttpAdapter HTTP_ADAPTER = null;
        public static final ContextualProvider PROVIDER = () -> Arrays.asList(
                Objects.requireNonNull(SERIALIZATION_ADAPTER, "Serialization Adapter is not defined"),
                Objects.requireNonNull(HTTP_ADAPTER, "HTTP Adapter is not defined"));
    }
}
