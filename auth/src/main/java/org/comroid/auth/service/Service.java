package org.comroid.auth.service;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.webkit.oauth.client.Client;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.java_websocket.util.Base64;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Service extends DataContainerBase<Service> implements Resource {
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

    @Override
    public String generateAccessToken(OAuthAuthorization authorization) {
        Client client = authorization.getClient();
        Resource resource = authorization.getResource();
        String code = String.format("%s-%s-%s", client.getUUID(), resource.getUUID(), UUID.randomUUID());
        code = Base64.encodeBytes(code.getBytes());
        return code;
    }
}
