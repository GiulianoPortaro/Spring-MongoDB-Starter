package com.mongodb.starter.database.repository;

import com.mongodb.starter.database.dto.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByMail(String mail);
    List<User> findByName(String name);
    List<User> findByNameAndSurname(String name, String surname);
    List<User> findByNameContaining(String subString);
}
