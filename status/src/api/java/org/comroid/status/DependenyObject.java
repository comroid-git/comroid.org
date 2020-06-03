package org.comroid.status;

import org.comroid.status.entity.Service;
import org.comroid.uniform.SerializationAdapter;

public interface DependenyObject {
    SerializationAdapter<?, ?, ?> SERIALIZATION_ADAPTER = null;

    String URL_BASE = "https://api.status.comroid.org/";

    static Service resolveService(DependenyObject dependenyObject, String name) {
        return null; //todo
    }
}
