package org.comroid.auth.repo;

import org.jetbrains.annotations.ApiStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.session.Session;

@ApiStatus.Internal
public interface SessionRepository extends CrudRepository<Session, String> {
}
