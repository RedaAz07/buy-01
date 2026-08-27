package com.microservices.product_service.service.kafka;

import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.microservices.product_service.dto.event.MediaUploadedEvent;
import com.microservices.product_service.model.Product;
import com.microservices.product_service.repository.productRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final productRepository productRepository;

    @KafkaListener(topics = "media-uploaded-topic", groupId = "product-service-group")
    public void handleMediaUploaded(MediaUploadedEvent event) {
        log.info("Kafka Event Received: Adding image URL to productId: {}", event.productId());
        Optional<Product> optionalProduct = productRepository.findById(event.productId());
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            if (product.getImageUrls() == null) {
                product.setImageUrls(new java.util.ArrayList<>());
            }
            product.getImageUrls().add(event.imageUrl());
            productRepository.save(product);
            log.info("Image URL added to product {}: {}", event.productId(), event.imageUrl());
        } else {
            log.warn("Product not found for productId: {}", event.productId());
        }
    }
}
