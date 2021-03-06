package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.EMailAddress;
import org.comroid.api.Rewrapper;
import org.comroid.api.Serializer;
import org.comroid.auth.model.AuthEntity;
import org.comroid.auth.service.Service;
import org.comroid.common.io.FileHandle;
import org.comroid.common.io.FileProcessor;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.restless.MimeType;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.Bitmask;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.webkit.oauth.client.Client;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class UserAccount extends DataContainerBase<AuthEntity> implements Client, FileProcessor, User {
    @RootBind
    public static final GroupBind<UserAccount> Type = User.Type.subGroup("user-account");
    public static final VarBind<UserAccount, Boolean, Boolean, Boolean> EMAIL_VERIFIED
            = Type.createBind("email_verified")
            .extractAs(StandardValueType.BOOLEAN)
            .asIdentities()
            .onceEach()
            .setDefaultValue(() -> false)
            .build();
    public static final VarBind<UserAccount, String, String, String> INTERNAL_EMAIL
            = Type.createBind("internal_email")
            .extractAs(StandardValueType.STRING)
            .build();
    public static final String ORG_EMAIL_SUFFIX = "@comroid.org";
    private static final Logger logger = LogManager.getLogger();
    public final Ref<UUID> id = getComputedReference(ID);
    public final Ref<String> username = getComputedReference(USERNAME);
    public final Ref<EMailAddress> email = getComputedReference(EMAIL);
    public final Ref<Boolean> emailVerified = getComputedReference(EMAIL_VERIFIED);
    public final Ref<String> internalEmail = getComputedReference(INTERNAL_EMAIL);
    public final Ref<Permit.Set> permits = getComputedReference(User.PERMIT);
    private final FileHandle dir;
    private final FileHandle loginHashFile;
    private final HashSet<OAuthAuthorization> authorizationTokens;
    private final HashSet<OAuthAuthorization.AccessToken> accessTokens;
    private final UserDataStorage.ServiceMap dataStorage;

    {
        // use Email as Username if no username is provided
        if (username.isNull())
            username.rebind(email.map(EMailAddress::toString));

        if (email.contentEquals("burdoto@outlook.com"))
            put(User.PERMIT, Bitmask.combine(Permit.values()));
        else if (permits.test(Set::isEmpty))
            put(User.PERMIT, Bitmask.combine(Permit.EMAIL, Permit.STORAGE));

        if (getPermits().contains(Permit.DEV))
            if (getEmail().endsWith(ORG_EMAIL_SUFFIX))
                // force internal email override by org-email
                getExtractionReference(INTERNAL_EMAIL)
                        .rebind(email.map(EMailAddress::toString)
                                .map(ReferenceList::of));
            else if (username.test(user -> !user.contains("@"))) // else create new org-email
                put(INTERNAL_EMAIL, username.into(usr -> usr.toLowerCase() + ORG_EMAIL_SUFFIX));
    }

    public FileHandle getDirectory() {
        return dir;
    }

    @Override
    public UUID getUUID() {
        return id.assertion("ID not found");
    }

    @Override
    public String getName() {
        return getUsername();
    }

    public String getEmail() {
        return getEMailAddress().toString();
    }

    public @Nullable String getInternalEmail() {
        return internalEmail.get();
    }

    @Override
    public UniNode getUserInfo() {
        UniObjectNode data = toUniNode();

        data.put("sub", getUUID());
        //data.put("email_verified", false);

        return data;
    }

    @Override
    @Deprecated
    public FileHandle getDataDirectory() {
        return getFile();
    }

    @Override
    public FileHandle getFile() {
        return dir;
    }

    public UserDataStorage getDataStorage(UUID serviceId) {
        return dataStorage.getForService(serviceId);
    }

    public boolean isEmailVerified() {
        return emailVerified.assertion("emailVerified");
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
        this.authorizationTokens = new HashSet<>();
        this.accessTokens = new HashSet<>();
        this.dataStorage = new UserDataStorage.ServiceMap(this);
        addChildren(dataStorage);
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
        this.authorizationTokens = new HashSet<>();
        this.accessTokens = new HashSet<>();
        this.dataStorage = new UserDataStorage.ServiceMap(this);
        addChildren(dataStorage);
    }

    public static String encrypt(String saltName, String input) {
        try {
            byte[] bytes = UserManager.getSalt(saltName);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(bytes);
            byte[] hashedPassword = md.digest(input.getBytes(StandardCharsets.US_ASCII));
            String hash = new String(hashedPassword).replace('\r', '#').replace('\n', '#');
            //logger.info("Encrypting: this.email = {}; this.hash = {}; password = {}", email, hash, password);
            return hash;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public int storeData() throws IOException {
        String string = toSerializedString();
        dir.createSubFile("user.json").setContent(string);
        return 1;
    }

    @Override
    public int reloadData() throws IOException {
        String data = dir.createSubFile("user.json").getContent();
        UniObjectNode obj = (UniObjectNode) findSerializer(MimeType.JSON).parse(data);
        return updateFrom(obj).size();
    }

    public Rewrapper<OAuthAuthorization> findAuthorization(final String code) {
        return () -> authorizationTokens.stream()
                .filter(authorization -> authorization.code.contentEquals(code))
                .findAny()
                .orElse(null);
    }

    public Rewrapper<OAuthAuthorization.AccessToken> findAccessToken(final String token) throws RestEndpointException {
        return () -> accessTokens.stream()
                .filter(OAuthAuthorization.AccessToken::isValid)
                .filter(access -> access.checkToken(token))
                .findAny()
                .orElse(null);
    }

    public boolean tryLogin(EMailAddress email, String password) {
        if (!this.email.contentEquals(email)) {
            logger.error("Email Mismatch: {} / {}", this.email.get(), email);
            return false;
        }
        String hash = encrypt(email.toString(), password);
        String otherHash = this.loginHashFile.getContent();
        String mail = this.email.into(EMailAddress::toString);
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

    public OAuthAuthorization createOAuthSession(Context context, Service service, String userAgent, Permit.Set scopes) {
        OAuthAuthorization oAuthAuthorization = new OAuthAuthorization(context, this, service, userAgent, scopes.toStringArray());
        authorizationTokens.add(oAuthAuthorization);
        return oAuthAuthorization;
    }

    @Override
    public boolean addAccessToken(OAuthAuthorization.AccessToken accessToken) {
        return accessTokens.add(accessToken);
    }

    @Override
    public boolean checkScopes(Set<String> scopes) {
        checkPermits(Permit.valueOf(scopes.toArray(new String[0])));
        return true;
    }

    @Override
    public OAuthAuthorization createAuthorization(Context context, Resource resource, String userAgent, Set<String> scopes) {
        return createOAuthSession(context, (Service) resource, userAgent, Permit.valueOf(scopes.toArray(new String[0])));
    }

    @Override
    public String generateAuthorizationToken(Resource resource, String userAgent) {
        return String.format("%s-%s-%s", getUUID(), resource.getUUID(), UUID.randomUUID());
    }

    public Stream<OAuthAuthorization.AccessToken> findToken(String token) {
        return accessTokens.stream().filter(it -> it.token.contentEquals(token));
    }
}
