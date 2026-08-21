package com.microservices.product_service.dto;

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
    private String userId;
    private long quantity;
}
