package org.comroid.auth.service;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.auth.server.AuthServer;
import org.comroid.api.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FileBasedService extends DataBasedService {
    @RootBind
    public static final GroupBind<Service> Type = Service.Type.subGroup("file-service");
    public static final FileHandle DIR = AuthServer.DATA.createSubDir("services");
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> name = getComputedReference(NAME);
    private final FileHandle dir;
    private final FileHandle secretFile;

    @Override
    public String getSecret() {
        return secretFile.getContent();
    }

    protected FileBasedService(ContextualProvider context, final FileHandle sourceDir) {
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
        this.secretFile = dir.createSubFile("secret.cred");
        initiateSecret();
    }

    protected FileBasedService(ContextualProvider context, @Nullable UniObjectNode initialData) {
        super(context, initialData);
        this.dir = DIR.createSubDir(id.toString());
        this.secretFile = dir.createSubFile("secret.cred");
        initiateSecret();
        dir.createSubFile("service.json").setContent(toSerializedString());
    }

    private void initiateSecret() {
        String content = secretFile.getContent();
        if (content == null || content.isEmpty())
            secretFile.setContent(UUID.randomUUID().toString().replace("-", ""));
    }
}
