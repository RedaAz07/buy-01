package com.microservices.product_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.microservices.product_service.dto.productRequest;
import com.microservices.product_service.dto.productRspons;
import com.microservices.product_service.service.productService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class productController {

    private final productService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    private void AddProduct(productRequest productRspons) {
        productService.AddProduct(productRspons);
    }

    @GetMapping
    private List<productRspons> GetallProduct(){
        return productService.getall();
    }
}
