package org.comroid.auth.user;

import org.comroid.api.Serializer;
import org.comroid.api.UUIDContainer;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

import java.util.UUID;

public final class UserAccount extends DataContainerBase<UserAccount> implements UUIDContainer {
    @RootBind
    public static final GroupBind<UserAccount> Type = new GroupBind<>(AuthServer.MASTER_CONTEXT, "user-account");
    public static final VarBind<UserAccount, String, UUID, UUID> ID
            = Type.createBind("uuid")
            .extractAs(StandardValueType.STRING)
            .andRemap(UUID::fromString)
            .build();
    public static final VarBind<UserAccount, String, String, String> EMAIL
            = Type.createBind("email")
            .extractAs(StandardValueType.STRING)
            .build();
    public static final VarBind<UserAccount, String, String, String> PASSWORD
            = Type.createBind("password")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> email = getComputedReference(EMAIL);
    public final Ref<String> password = getComputedReference(PASSWORD);
    private final FileHandle dir;

    @Override
    public UUID getUUID() {
        return id.assertion("ID not found");
    }

    @Override
    public String getName() {
        return email.assertion("Email not found");
    }

    UserAccount(UserManager context, final FileHandle sourceDir) {
        super(context, obj -> {
            if (!sourceDir.isDirectory())
                throw new IllegalArgumentException(String.format("File is not a directory: %s", sourceDir));
            if (!sourceDir.mkdir())
                throw new IllegalArgumentException(String.format("Could not create user directory %s", sourceDir));
            FileHandle subFile = sourceDir.createSubFile("user.json");
            if (!subFile.exists())
                throw new IllegalArgumentException(String.format("Source directory has no user configuration: %s", sourceDir));
            obj.copyFrom(subFile.parse(context.requireFromContext(Serializer.class)));
        });
        this.dir = sourceDir;
    }

    UserAccount(UserManager context, UUID id, String email, String password) {
        super(context, obj -> {
            obj.put(ID, id.toString());
            obj.put(EMAIL, email);
            obj.put(PASSWORD, password);
        });
        this.dir = UserManager.DIR.createSubDir(id.toString());
        dir.mkdir();
        dir.createSubFile("user.json").setContent(toSerializedString());
    }
}
