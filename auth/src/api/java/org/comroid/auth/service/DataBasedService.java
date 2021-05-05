package org.comroid.auth.service;

import org.comroid.api.ContextualProvider;
import org.comroid.auth.model.AuthEntity;
import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DataBasedService extends DataContainerBase<AuthEntity> implements Service {
    @RootBind
    public static final GroupBind<DataBasedService> Type
            = Service.Type.subGroup("service-impl");
    public static final VarBind<DataBasedService, String, String, String> SECRET
            = Type.createBind("secret")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<String> secret = getComputedReference(SECRET);

    @Override
    public @Nullable String getSecret() {
        return get(SECRET);
    }

    public DataBasedService(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
    }

    protected DataBasedService(ContextualProvider context, Consumer<UniObjectNode> initialDataBuilder) {
        super(context, initialDataBuilder);
    }
}
