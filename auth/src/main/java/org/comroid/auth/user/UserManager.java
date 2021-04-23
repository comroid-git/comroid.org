package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;
import org.comroid.oauth.user.OAuthAuthorization;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class UserManager implements ContextualProvider.Underlying, UncheckedCloseable {
    public static final FileHandle DIR = AuthServer.DATA.createSubDir("users");
    public static final FileHandle SALTS = AuthServer.DATA.createSubDir("salts");
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, byte[]> salts = new ConcurrentHashMap<>();

    static {
        DIR.mkdir();
        SALTS.mkdir();
    }

    private final Map<UUID, UserAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final ContextualProvider context;

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public Collection<UserAccount> getUsers() {
        return accounts.values();
    }

    public UserManager(AuthServer server) {
        this.context = server.plus("UserManager", this);

        Stream.of(DIR.listFiles())
                .filter(File::isDirectory)
                .filter(dir -> {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        UUID.fromString(dir.getName());
                        return true;
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .map(FileHandle::new)
                .map(dir -> new UserAccount(this, dir))
                .forEach(account -> accounts.put(account.getUUID(), account));
        logger.info("Loading finished; loaded {} user accounts", accounts.size());
    }

    public static byte[] getSalt(String root) {
        return salts.computeIfAbsent(root, k -> {
            FileHandle f = SALTS.createSubFile(k + ".salt");
            if (f.exists())
                return f.getContent(false).getBytes();
            if (!f.exists() && !f.validateExists())
                throw new RuntimeException();
            String base = root + UUID.randomUUID();
            f.setContent(base);
            return base.getBytes();
        });
    }

    public UserAccount createAccount(String email, String password) {
        logger.info("Creating new user account with email " + email);

        if (accounts.values()
                .stream()
                .flatMap(usr -> usr.email.stream())
                .anyMatch(email::equalsIgnoreCase))
            throw new IllegalArgumentException("E-Mail is already in use!");

        UUID uuid = UUID.randomUUID();
        UserAccount account = new UserAccount(this, uuid, email, password);
        accounts.put(uuid, account);
        return account;
    }

    public UserSession loginUser(String email, String password) {
        logger.info("User {} logging in...", email);

        return accounts.values()
                .stream()
                .filter(usr -> usr.email.contentEquals(email))
                .findAny()
                .filter(usr -> usr.tryLogin(email, password))
                .map(UserSession::new)
                .filter(session -> sessions.put(session.getPlainCookie(), session) != session)
                .orElseThrow(() -> new IllegalArgumentException("Could not authenticate"));
    }

    public UserSession findSession(String cookie) {
        UserSession session = sessions.getOrDefault(cookie, null);
        if (session == null)
            throw new IllegalArgumentException("No session found with given cookie");
        return session;
    }

    public OAuthAuthorization findOAuthAuthorization(final String authorizationCode) throws RestEndpointException {
        return accounts.values()
                .stream()
                .flatMap(account -> account.findAuthorization(authorizationCode).stream())
                .findAny()
                .orElseThrow(() -> new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Invalid Token used"));
    }

    public UserAccount findOAuthSession(REST.Header.List headers) throws RestEndpointException {
        final String token = findToken(headers);
        return accounts.values()
                .stream()
                .flatMap(account -> account.findAccessToken(token).stream())
                .findAny()
                .map(OAuthAuthorization.AccessToken::getAuthorization)
                .map(OAuthAuthorization::getAccount)
                .orElseThrow(() -> new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Could not authenticate using token"));
    }

    private String findToken(REST.Header.List headers) {
        String token = headers.getFirst(CommonHeaderNames.AUTHORIZATION);

        if (token == null)
            throw new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "No Token found");
        if (!token.startsWith(OAuthAuthorization.BEARER_PREFIX))
            throw new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Invalid Token type");
        return token.substring(OAuthAuthorization.BEARER_PREFIX.length());
    }

    public boolean closeSession(UserSession session) {
        return sessions.remove(session.getCookie()) != session;
    }

    @Override
    public void close() {
    }

    public Rewrapper<UserAccount> getUser(UUID uuid) {
        return () -> accounts.getOrDefault(uuid, null);
    }
}
