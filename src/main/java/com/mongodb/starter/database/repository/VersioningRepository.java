package com.mongodb.starter.database.repository;

import com.mongodb.starter.database.dto.Versioning;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VersioningRepository extends MongoRepository<Versioning, String> {
}
