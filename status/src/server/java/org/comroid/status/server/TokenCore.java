package org.comroid.status.server;

import org.comroid.status.entity.Service;

import java.util.Base64;
import java.util.UUID;

public final class TokenCore {
    public static synchronized String generate(String entityName) {
        String token = entityName + ':';

        token += UUID.randomUUID().toString();
        token += ':';
        token += UUID.randomUUID().toString();

        final Base64.Encoder encoder = Base64.getEncoder();
        final String yield = encoder.encodeToString(token.getBytes());

        if (!isValid(yield))
            throw new AssertionError("Generated token is invalid");

        return yield;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isValid(String token) {
        final Base64.Decoder decoder = Base64.getDecoder();
        final String decoded = new String(decoder.decode(token));

        final String[] parts = decoded.split(":");

        if (parts.length != 3)
            return false;
        try {
            UUID.fromString(parts[1]);
            UUID.fromString(parts[2]);
        } catch (Throwable ignored) {
            return false;
        }

        return true;
    }

    public static String extractName(String token) {
        final Base64.Decoder decoder = Base64.getDecoder();
        final String decoded = new String(decoder.decode(token));

        return decoded.substring(0, decoded.indexOf(':'));
    }
}
