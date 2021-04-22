package org.comroid.auth.model;

import org.comroid.auth.user.Permit;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.server.RestEndpointException;

import java.util.Set;
import java.util.stream.Stream;

public interface PermitCarrier {
    Set<Permit> getPermits();

    default boolean hasPermits(Permit... permits) {
        return Stream.of(permits).allMatch(getPermits()::contains);
    }

    default void checkPermits(Permit... permits) throws RestEndpointException {
        if (!hasPermits(permits))
            throw new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Insufficient Permit");
    }
}
