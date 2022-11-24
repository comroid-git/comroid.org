package org.comroid.auth.repo;

import org.comroid.auth.entity.AuthService;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ServiceRepository extends CrudRepository<AuthService, UUID> {
}
