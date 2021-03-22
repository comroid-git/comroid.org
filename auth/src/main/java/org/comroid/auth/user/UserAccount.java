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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public static final VarBind<UserAccount, String, String, String> LOGIN
            = Type.createBind("login")
            .extractAs(StandardValueType.STRING)
            .build();
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> email = getComputedReference(EMAIL);
    public final Ref<String> login = getComputedReference(LOGIN);
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
            if (!sourceDir.exists() && !sourceDir.mkdir())
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
            obj.put(LOGIN, encrypt(email, password));
        });
        this.dir = UserManager.DIR.createSubDir(id.toString());
        dir.mkdir();
        dir.createSubFile("user.json").setContent(toSerializedString());
    }

    public static String encrypt(String email, String password) {
        try {
            byte[] bytes = UserManager.getSalt(email);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(bytes);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.US_ASCII));
            return new String(hashedPassword);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public boolean tryLogin(String email, String password) {
        String result = encrypt(email, password);
        return login.contentEquals(result);
    }
}
