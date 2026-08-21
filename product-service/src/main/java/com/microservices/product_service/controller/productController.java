package com.microservices.product_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private void AddProduct(@RequestBody productRequest productRequest) {
        productService.AddProduct(productRequest);
    }

    @GetMapping
    private List<productRspons> GetallProduct() {
        return productService.getall();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    private void UpdateProduct(@RequestBody productRequest productRequest, @PathVariable("id") String id) {
        productService.UpdateProduct(productRequest, id);
    }

    @DeleteMapping("/{id}")
    private void DeleteProduct(@PathVariable("id") String id) {
        productService.DeleteProduct(id);

    }
}
