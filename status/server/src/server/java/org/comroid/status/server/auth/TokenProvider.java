package org.comroid.status.server.auth;

import org.comroid.status.server.StatusServer;
import org.comroid.status.server.exception.InvalidTokenException;
import org.comroid.status.server.repo.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.UUID;

@Service
public final class TokenProvider {
    @Autowired
    private TokenRepository tokens;

    @PostConstruct
    public void init() {
        if (!hasToken(StatusServer.ADMIN_TOKEN_NAME))
            generate(StatusServer.ADMIN_TOKEN_NAME);
    }

    public boolean hasToken(String name) {
        return tokens.findById(name).isPresent();
    }

    public synchronized Token generate(String name) {
        String token = name + ':';

        token += UUID.randomUUID().toString();

        final Base64.Encoder encoder = Base64.getEncoder();
        final String yield = encoder.encodeToString(token.getBytes());

        if (!isValid(yield))
            throw new AssertionError("Generated token is invalid");

        return tokens.save(new Token(name, yield));
    }

    public boolean isAuthorized(String name, String token) {
        try {
            return isAuthorized(name, token, false);
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }

    private boolean isAuthorized(String name, String token, boolean recursive) {
        String insideName = extractName(token);
        if (!recursive && insideName.equals(StatusServer.ADMIN_TOKEN_NAME))
            return isAuthorized(StatusServer.ADMIN_TOKEN_NAME, token, true);
        if (!isValid(token))
            return false;
        if (!insideName.equals(name))
            return false;
        return tokens.findById(name).map(tk -> tk.getToken().equals(token)).orElse(true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean isValid(String token) {
        final Base64.Decoder decoder = Base64.getDecoder();
        final String decoded = new String(decoder.decode(token));

        final String[] parts = decoded.split(":");

        if (parts.length != 2)
            return false;
        try {
            UUID.fromString(parts[1]);
        } catch (Throwable ignored) {
            return false;
        }

        return true;
    }

    public String extractName(String token) {
        final Base64.Decoder decoder = Base64.getDecoder();
        final String decoded = new String(decoder.decode(token));

        return decoded.substring(0, decoded.indexOf(':'));
    }
}
