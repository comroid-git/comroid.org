package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Serializer;
import org.comroid.api.UUIDContainer;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.Ref;
import org.comroid.util.Bitmask;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
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
    public static final VarBind<UserAccount, Integer, Set<Permit>, Set<Permit>> PERMIT
            = Type.createBind("permit")
            .extractAs(StandardValueType.INTEGER)
            .andRemap(Permit::valueOf)
            .onceEach()
            .setDefaultValue(Collections::emptySet)
            .build();
    private static final Logger logger = LogManager.getLogger();
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> email = getComputedReference(EMAIL);
    public final Ref<Set<Permit>> permits = getComputedReference(PERMIT);
    private final FileHandle dir;
    private final FileHandle loginHashFile;

    {
        if (email.contentEquals("burdoto@outlook.com"))
            put(PERMIT, Bitmask.combine(Permit.values()));
        else if (permits.test(Set::isEmpty))
            put(PERMIT, Permit.NONE.getValue());
    }

    @Override
    public UUID getUUID() {
        return id.assertion("ID not found");
    }

    @Override
    public String getName() {
        return email.assertion("Email not found");
    }

    public Set<Permit> getPermits() {
        return permits.assertion("Permits not found");
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
        this.loginHashFile = dir.createSubFile("login.hash");
    }

    UserAccount(UserManager context, UUID id, String email, String password) {
        super(context, obj -> {
            obj.put(ID, id.toString());
            obj.put(EMAIL, email);
        });
        this.dir = UserManager.DIR.createSubDir(id.toString());
        dir.mkdir();
        dir.createSubFile("user.json").setContent(toSerializedString());
        this.loginHashFile = dir.createSubFile("login.hash");
        this.loginHashFile.setContent(encrypt(email, password));
    }

    public static String encrypt(String email, String password) {
        try {
            byte[] bytes = UserManager.getSalt(email);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(bytes);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.US_ASCII));
            String hash = new String(hashedPassword).replace('\r', '#').replace('\n', '#');
            //logger.info("Encrypting: this.email = {}; this.hash = {}; password = {}", email, hash, password);
            return hash;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public boolean tryLogin(String email, String password) {
        if (!this.email.contentEquals(email)) {
            logger.error("Email Mismatch: {} / {}", this.email.get(), email);
            return false;
        }
        String hash = encrypt(email, password);
        String otherHash = this.loginHashFile.getContent();
        String mail = this.email.get();
        byte[] bytes1 = hash.getBytes(StandardCharsets.US_ASCII);
        byte[] bytes2 = otherHash.getBytes(StandardCharsets.US_ASCII);
        boolean equals = Arrays.equals(bytes1, bytes2);
        //logger.info("Logging in: this.email = {}; this.hash = {}; password = {}", mail, hash, password);
        //logger.info("Other Data: othr.email = {}; othr.hash = {}; equals = {}", email, otherHash, equals);
        //logger.info("Array 1: {}", Arrays.toString(bytes1));
        //logger.info("Array 2: {}", Arrays.toString(bytes2));
        return equals;
    }

    public void putHash(String hash) {
        loginHashFile.setContent(hash);
    }
}
