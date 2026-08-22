package com.microservices.product_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class productRspons {
    private String id;
    private String name;
    private String description;
    private long price;
    private int quantity;
    private String sellerId;
    private List<String> imageUrls;
}
