package com.microservices.product_service.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class productRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must not exceed 120 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Positive(message = "Price must be greater than zero")
    private long price;

    @NotBlank(message = "Seller ID is required")
    private String sellerId;

    @PositiveOrZero(message = "Quantity must be zero or greater")
    private int quantity;

    @NotNull(message = "Image URLs are required; use an empty array when there are no images")
    @Size(max = 10, message = "A product can have at most 10 images")
    @Builder.Default
    private List<@NotBlank(message = "Image reference must not be blank") String> imageUrls = new java.util.ArrayList<>();
}
