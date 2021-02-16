package org.comroid.status;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.HttpAdapter;
import org.comroid.uniform.SerializationAdapter;

public final class AdapterDefinition extends ContextualProvider.Base {
    public static final String URL_BASE = "https://api.status.comroid.org/";
    public static final Reference<AdapterDefinition> instance = Reference.create();
    public final SerializationAdapter<?, ?, ?> serialization;
    public final HttpAdapter http;

    private AdapterDefinition(SerializationAdapter<?, ?, ?> serialization, HttpAdapter http) {
        super(serialization, http);

        this.serialization = serialization;
        this.http = http;
    }

    public static AdapterDefinition getInstance() {
        return instance.get();
    }

    public static AdapterDefinition initialize(SerializationAdapter<?,?,?> serialization, HttpAdapter http) {
        return instance.compute(old -> new AdapterDefinition(serialization, http));
    }
}
