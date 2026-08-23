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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class productController {

    private final productService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public productRspons AddProduct(@RequestBody @Valid productRequest productRequest) {
        return productService.AddProduct(productRequest);
    }

    @GetMapping
    public List<productRspons> GetallProduct() {
        System.out.println("11");
        return productService.getall();
    }

    @GetMapping("/{id}")
    public productRspons GetProduct(@PathVariable String id) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    public productRspons UpdateProduct(@RequestBody @Valid productRequest productRequest, @PathVariable("id") String id) {
        return productService.UpdateProduct(productRequest, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void DeleteProduct(@PathVariable("id") String id) {
        productService.DeleteProduct(id);

    }
}
