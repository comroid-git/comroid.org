package org.comroid.status;

import org.comroid.restless.HttpAdapter;
import org.comroid.status.entity.Service;
import org.comroid.uniform.SerializationAdapter;
import org.jetbrains.annotations.NotNull;

public interface DependenyObject {
    @SuppressWarnings("ConstantConditions") // must be defined by the user
    @NotNull SerializationAdapter<?, ?, ?> SERIALIZATION_ADAPTER = null;
    @SuppressWarnings("ConstantConditions") // must be defined by the user
    @NotNull HttpAdapter HTTP_ADAPTER = null;

    String URL_BASE = "https://api.status.comroid.org/";

    static Service resolveService(DependenyObject dependenyObject, String name) {
        return null; //todo
    }
}
