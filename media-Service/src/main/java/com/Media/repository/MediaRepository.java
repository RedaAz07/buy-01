package com.Media.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Media.model.Media;
import com.Media.model.UploadType;

public interface MediaRepository extends MongoRepository<Media, String> {
    Optional<Media> findById(String id);

    Optional<Media> findByUserIdAndOwnerId(String id, String ownerId);

}