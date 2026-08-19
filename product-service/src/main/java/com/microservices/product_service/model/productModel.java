package com.microservices.product_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;


@Data
@Document(value="product")
@Builder
public class productModel {
    @Id
    private String id;
    private String name;
    private String description;
    private long price;
}
