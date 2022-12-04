package org.comroid.status.server.repo;

import org.comroid.status.server.auth.Token;
import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, String> {
}
