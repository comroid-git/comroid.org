package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.EMailAddress;
import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.util.Pair;
import org.comroid.webkit.oauth.client.Client;
import org.comroid.webkit.oauth.client.ClientProvider;
import org.comroid.webkit.oauth.model.ValidityStage;
import org.comroid.webkit.oauth.user.OAuthAuthorization;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class UserManager implements ContextualProvider.Underlying, UncheckedCloseable, ClientProvider {
    public static final FileHandle DIR = AuthServer.DATA.createSubDir("users");
    public static final FileHandle SALTS = AuthServer.DATA.createSubDir("salts");
    private static final Logger logger = LogManager.getLogger();
    private static final Map<String, byte[]> salts = new ConcurrentHashMap<>();

    static {
        DIR.mkdir();
        SALTS.mkdir();
    }

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
                .map(EMailAddress::toString)
                .anyMatch(email::equalsIgnoreCase))
            throw new IllegalArgumentException("E-Mail is already in use!");

        UUID uuid = UUID.randomUUID();
        UserAccount account = new UserAccount(this, uuid, email, password);
        accounts.put(uuid, account);
        return account;
    }

    public UserSession loginUser(EMailAddress email, String password) {
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

    @Override
    public OAuthAuthorization findAuthorization(final String authorizationCode) throws RestEndpointException {
        if (authorizationCode == null)
            throw new IllegalArgumentException("authorization code cannot be null");
        return accounts.values()
                .stream()
                .flatMap(account -> account.findAuthorization(authorizationCode).stream())
                .findAny()
                .orElseThrow(() -> new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Invalid Token used"));
    }

    @Override
    public OAuthAuthorization.AccessToken findAccessToken(final String token) throws RestEndpointException {
        if (token == null)
            throw new IllegalArgumentException("token cannot be null");
        return accounts.values()
                .stream()
                .flatMap(account -> account.findAccessToken(token).stream())
                .findAny()
                .orElseThrow(() -> new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Could not authenticate using token"));
    }

    @Override
    public boolean hasClient(UUID uuid) {
        return accounts.containsKey(uuid);
    }

    @Override
    public Rewrapper<UserAccount> findClient(REST.Header.List headers) {
        return () -> UserSession.findSession(headers).getAccount();
    }

    @Override
    public Rewrapper<UserAccount> findClient(UUID uuid) {
        return () -> accounts.getOrDefault(uuid, null);
    }

    @Override
    public Pair<Client, String> loginClient(EMailAddress email, String login) {
        UserSession session = AuthServer.instance.getUserManager()
                .loginUser(email, login);
        return new Pair<>(session.getAccount(), session.getCookie());
    }

    @Override
    public ValidityStage findValidityStage(String token) {
        return accounts.values().stream()
                .flatMap(acc -> acc.findToken(token))
                .findAny()
                .orElse(null);
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
