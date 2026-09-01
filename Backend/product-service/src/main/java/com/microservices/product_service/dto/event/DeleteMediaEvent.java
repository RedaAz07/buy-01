package com.microservices.product_service.dto.event;

import com.microservices.product_service.service.kafka.UploadType;

public record DeleteMediaEvent(String userId, String productId, String imageUrl, UploadType type) {
}