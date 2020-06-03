package org.comroid.status;

import org.comroid.restless.HttpAdapter;
import org.comroid.status.entity.Service;
import org.comroid.uniform.SerializationAdapter;
import org.jetbrains.annotations.NotNull;

public interface DependenyObject {
    String URL_BASE = "https://api.status.comroid.org/";

    static Service resolveService(DependenyObject dependenyObject, String name) {
        return null; //todo
    }

    final class Adapters {
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull SerializationAdapter<?, ?, ?> SERIALIZATION_ADAPTER = null;
        @SuppressWarnings("ConstantConditions") // must be defined by the user
        public static @NotNull HttpAdapter HTTP_ADAPTER = null;
    }
}
