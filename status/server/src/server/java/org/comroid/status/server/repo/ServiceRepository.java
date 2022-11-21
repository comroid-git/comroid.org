package org.comroid.status.server.repo;

import org.comroid.status.entity.Service;
import org.springframework.data.repository.CrudRepository;

public interface ServiceRepository extends CrudRepository<Service, String> {
}
