package com.Media.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.Media.model.Media;

public interface MediaRepository extends MongoRepository<Media, String> {

}