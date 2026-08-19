package com.user_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.user_service.model.User;

public interface UserRespository extends MongoRepository<User, String> {
    Optional<User> findByName(String username);

}
