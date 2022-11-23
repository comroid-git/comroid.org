package org.comroid.auth.repo;

import org.comroid.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends CrudRepository<UserAccount, UUID> {
    @Query("select u from UserAccount u where u.sessionId = ?1")
    Optional<UserAccount> findBySessionId(String sessionId);
}
