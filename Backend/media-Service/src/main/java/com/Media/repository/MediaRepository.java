package com.Media.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Media.model.Media;
import com.Media.model.UploadType;

public interface MediaRepository extends MongoRepository<Media, String> {
    Optional<Media> findById(String id);

    void deleteByOwnerId(String id);

    void deleteByProductId(String id);

    Optional<Media> findByIdAndOwnerId(String id, String ownerId);
    Optional<Media> findByOwnerIdAndType(
    String ownerId,
    UploadType type
);

}