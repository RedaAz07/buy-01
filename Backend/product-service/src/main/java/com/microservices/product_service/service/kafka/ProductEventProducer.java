package com.microservices.product_service.service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.microservices.product_service.dto.event.ProductDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProductDeletedEvent(String productId) {
        ProductDeletedEvent event = new ProductDeletedEvent(productId);
        log.info("Publishing ProductDeletedEvent for productId: {}", productId);
        kafkaTemplate.send("product-deleted-topic", productId, event);
    }
}
