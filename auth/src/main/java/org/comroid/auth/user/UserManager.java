package org.comroid.auth.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.UncheckedCloseable;
import org.comroid.auth.server.AuthServer;
import org.comroid.common.io.FileHandle;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class UserManager implements ContextualProvider.Underlying, UncheckedCloseable {
    private static final Logger logger = LogManager.getLogger("UserManager");
    public static final FileHandle DIR = AuthServer.DIR.createSubDir("users");
    private final Map<UUID, UserAccount> accounts = new ConcurrentHashMap<>();
    private final ContextualProvider context;

    static {
        DIR.mkdir();
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
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

    public UserAccount createAccount(String email, String password) {
        logger.info("Creating new user account with email " + email);

        UUID uuid = UUID.randomUUID();
        UserAccount account = new UserAccount(this, uuid, email, password);
        accounts.put(uuid, account);
        return account;
    }

    @Override
    public void close() {
    }
}
