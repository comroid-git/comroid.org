package org.comroid.auth.user;

import org.comroid.auth.user.Permit;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.exception.RestEndpointException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

public interface PermitCarrier {
    Set<Permit> getPermits();

    default boolean hasPermits(Collection<Permit> permits) {
        return hasPermits(permits.toArray(new Permit[0]));
    }

    default boolean hasPermits(Permit... permits) {
        return Stream.of(permits).allMatch(getPermits()::contains);
    }

    default void checkPermits(Collection<Permit> permits) throws RestEndpointException {
        checkPermits(permits.toArray(new Permit[0]));
    }

    default void checkPermits(Permit... permits) throws RestEndpointException {
        if (!hasPermits(permits))
            throw new RestEndpointException(HTTPStatusCodes.UNAUTHORIZED, "Insufficient Permit");
    }
}
