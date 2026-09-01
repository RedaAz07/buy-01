package com.microservices.product_service.service.kafka;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.microservices.product_service.dto.event.DeleteMediaEvent;
import com.microservices.product_service.model.Product;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service
public class ProductMediaEventListener {

    private final MongoTemplate mongoTemplate;

    public ProductMediaEventListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @KafkaListener(topics = "media-deleted-topic", groupId = "product-group")
    public void handleMediaDeleted(DeleteMediaEvent event) {
        if (event.productId() == null || event.type() != UploadType.PRODUCT_IMAGE) {
            return;
        }

        Query query = new Query(Criteria.where("id").is(event.productId()));
        Update update = new Update().pull("imageUrls", event.imageUrl());

        mongoTemplate.updateFirst(query, update, Product.class);
    }
}