package org.comroid.auth.repo;

import org.comroid.auth.entity.AuthService;
import org.springframework.data.repository.CrudRepository;

public interface ServiceRepository extends CrudRepository<AuthService, String> {
}
