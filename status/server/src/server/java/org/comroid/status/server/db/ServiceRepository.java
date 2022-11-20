package org.comroid.status.server.db;

import org.comroid.status.entity.Service;
import org.springframework.data.repository.CrudRepository;

public interface ServiceRepository extends CrudRepository<Service, String> {
}
