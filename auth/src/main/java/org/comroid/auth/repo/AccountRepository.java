package org.comroid.auth.repo;

import org.comroid.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends CrudRepository<UserAccount, UUID> {
    @Query("select u from UserAccount u where u.sessionId = ?1")
    Optional<UserAccount> findBySessionId(String sessionId);

    @Query("select u from UserAccount u where u.username = ?1")
    Optional<UserAccount> findByUsername(String username);

    @Query("select u from UserAccount u where u.email = ?1")
    Optional<UserAccount> findByEmail(String email);

    @Query("select u from UserAccount u where u.emailVerifyCode = ?1")
    Optional<UserAccount> findByEmailVerificationCode(String code);

    @Query("select u from UserAccount u where u.changePasswordCode = ?1")
    Optional<UserAccount> findByPasswordUpdateCode(String code);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update UserAccount u set u.sessionId = ?2 where u.uuid = ?1")
    void setSessionId(UUID id, String sessionId);
}
