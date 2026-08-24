package com.Media.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Media.model.Media;

public interface MediaRepository extends MongoRepository<Media, String> {
    Optional<Media> findById(String id);

    Optional<Media> findByIdAndOwnerId(String id, String ownerId);

}