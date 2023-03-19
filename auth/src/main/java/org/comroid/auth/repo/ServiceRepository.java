package org.comroid.auth.repo;

import jakarta.annotation.PostConstruct;
import org.comroid.auth.entity.AuthService;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ServiceRepository extends CrudRepository<AuthService, String> {
    @PostConstruct
    @Transactional
    @Modifying
    @Query("update AuthService a set a.secretExpiry = 0")
    void migrateDB();
}
