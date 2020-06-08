package org.comroid.status.server;

import java.util.Base64;
import java.util.UUID;

public final class TokenCore {
    public static synchronized String generate(String entityName) {
        String token = entityName + ':';

        token += UUID.randomUUID().toString();
        token += UUID.randomUUID().toString();

        final Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(token.getBytes());
    }

    public static String extractName(String token) {
        final Base64.Decoder decoder = Base64.getDecoder();
        final String decoded = new String(decoder.decode(token));

        return decoded.substring(0, decoded.indexOf(':'));
    }
}
