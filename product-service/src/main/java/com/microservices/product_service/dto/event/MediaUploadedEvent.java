package com.microservices.product_service.dto.event;

public record MediaUploadedEvent(String productId, String imageUrl) {}
