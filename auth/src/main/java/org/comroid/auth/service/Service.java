package org.comroid.auth.service;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.api.UUIDContainer;
import org.comroid.auth.server.AuthServer;
import org.comroid.auth.user.UserManager;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

import java.util.UUID;

public final class Service extends DataContainerBase<Service> implements UUIDContainer {
    @RootBind
    public static final GroupBind<Service> Type = new GroupBind<>(AuthServer.MASTER_CONTEXT, "auth-service");
    public static final VarBind<Service, String, UUID, UUID> ID
            = Type.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .build();
    public final Ref<UUID> id = getComputedReference(ID);
    private final FileHandle dir;

    @Override
    public UUID getUUID() {
        return id.assertion("ID");
    }

    protected Service(ContextualProvider context, final FileHandle sourceDir) {
        super(context, obj -> {
            if (!sourceDir.isDirectory())
                throw new IllegalArgumentException(String.format("File is not a directory: %s", sourceDir));
            if (!sourceDir.exists() && !sourceDir.mkdir())
                throw new IllegalArgumentException(String.format("Could not create user directory %s", sourceDir));
            FileHandle subFile = sourceDir.createSubFile("user.json");
            if (!subFile.exists())
                throw new IllegalArgumentException(String.format("Source directory has no user configuration: %s", sourceDir));
            obj.copyFrom(subFile.parse(context.requireFromContext(Serializer.class)));
        });
        this.dir = sourceDir;
    }

    protected Service(ContextualProvider context, UUID id) {
        super(context, obj -> obj.put(ID, id.toString()));
        this.dir = UserManager.DIR.createSubDir(id.toString());
        dir.mkdir();
        dir.createSubFile("service.json").setContent(toSerializedString());
    }
}
