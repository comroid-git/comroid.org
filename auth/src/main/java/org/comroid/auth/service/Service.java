package org.comroid.auth.service;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.api.Serializer;
import org.comroid.api.UUIDContainer;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.user.UserManager;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.FileHandler;

public final class Service extends DataContainerBase<Service> implements UUIDContainer, Named {
    @RootBind
    public static final GroupBind<Service> Type = new GroupBind<>(AuthServer.MASTER_CONTEXT, "auth-service");
    public static final VarBind<Service, String, UUID, UUID> ID
            = Type.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .onceEach()
            .setRequired()
            .build();
    public static final VarBind<Service, String, String, String> NAME
            = Type.createBind("name")
            .extractAs(StandardValueType.STRING)
            .asIdentities()
            .onceEach()
            .setRequired()
            .build();
    public static final FileHandle DIR = AuthServer.DATA.createSubDir("services");
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> name = getComputedReference(NAME);
    private final FileHandle dir;

    @Override
    public UUID getUUID() {
        return id.assertion("ID");
    }

    @Override
    public String getName() {
        return name.assertion("Display Name");
    }

    protected Service(ContextualProvider context, final FileHandle sourceDir) {
        super(context, obj -> {
            if (!sourceDir.isDirectory())
                throw new IllegalArgumentException(String.format("File is not a directory: %s", sourceDir));
            if (!sourceDir.exists() && !sourceDir.mkdir())
                throw new IllegalArgumentException(String.format("Could not create service directory %s", sourceDir));
            FileHandle subFile = sourceDir.createSubFile("service.json");
            if (!subFile.exists())
                throw new IllegalArgumentException(String.format("Source directory has no service configuration: %s", sourceDir));
            obj.copyFrom(subFile.parse(context.requireFromContext(Serializer.class)));
        });
        this.dir = sourceDir;
    }

    protected Service(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
        this.dir = DIR.createSubDir(id.toString());
        dir.createSubFile("service.json").setContent(toSerializedString());
    }
}
