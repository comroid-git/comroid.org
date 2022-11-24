package org.comroid.auth.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.session.Session;

public interface SessionRepository extends CrudRepository<Session, String> {
}
