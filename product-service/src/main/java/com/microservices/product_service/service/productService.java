package com.microservices.product_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.microservices.product_service.dto.productRequest;
import com.microservices.product_service.dto.productRspons;
import com.microservices.product_service.model.Product;
import com.microservices.product_service.repository.productRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class productService {
    private final productRepository productRepository;

    public void AddProduct(productRequest productRspons) {

        Product product = Product
                .builder()
                .name(productRspons.getName())
                .description(productRspons.getDescription())
                .price(productRspons.getPrice())
                .build();
        productRepository.save(product);
        System.out.println("product" + product.getId() + " has been save");

    }

    public List<productRspons> getall() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(p -> getProduct(p)).toList();
    }

    public productRspons getProduct(String id) {
        Product product = productRepository.findById(id).orElse(null);
        return getProduct(product);
    }

    private productRspons getProduct(Product p) {
        productRspons product = productRspons.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .build();
        return product;
    }

    public void UpdateProduct(productRequest productRequest, String id) {
        Product product = productRepository.findById(id).orElseThrow();
        System.out.println(productRequest.getPrice());
        product.setName(productRequest.getName());
        product.setPrice(productRequest.getPrice());
        product.setDescription(productRequest.getDescription());
        productRepository.save(product);

    }

    public void DeleteProduct(String id) {
        Product product = productRepository.findById(id).orElse(null);
        productRepository.delete(product);
    }
}
