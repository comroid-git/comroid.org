package org.comroid.auth.repo;

import org.comroid.auth.entity.AuthService;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

public interface ServiceRepository extends CrudRepository<AuthService, String> {
    @PostConstruct
    @Transactional
    @Modifying
    @Query("update AuthService a set a.secretExpiry = 0")
    void migrateDB();
}
