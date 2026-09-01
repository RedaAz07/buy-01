package com.microservices.product_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.microservices.product_service.dto.FeingResponse;
import com.microservices.product_service.dto.productRequest;
import com.microservices.product_service.dto.productRspons;
import com.microservices.product_service.exception.ApiException;
import com.microservices.product_service.model.Product;
import com.microservices.product_service.repository.productRepository;
import com.microservices.product_service.service.kafka.ProductEventProducer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class productService {
    private final productRepository productRepository;
    private final ProductEventProducer productEventProducer;

    public productRspons AddProduct(productRequest productRequest, String sellerId) {
        if (productRequest.getDescription() == null || productRequest.getDescription().isEmpty()
                || productRequest.getName() == null || productRequest.getName().isEmpty()
                || productRequest.getPrice() == 0 || productRequest.getQuantity() == 0) {
            throw ApiException.badRequest("all fields are required");

        }
        Product product = Product
                .builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .quantity(productRequest.getQuantity())
                .sellerId(sellerId)
                .imageUrls(new ArrayList<>(productRequest.getImageUrls()))
                .build();
        productRepository.save(product);
        return getProduct(product);
    }

    public List<productRspons> getMyProducts(String sellerId) {
        List<Product> products = productRepository.findBySellerId(sellerId);
        return products.stream().map(p -> getProduct(p)).toList();

    }

    public List<productRspons> getall() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(p -> getProduct(p)).toList();
    }

    public productRspons getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return getProduct(product);
    }

    private productRspons getProduct(Product p) {
        return productRspons.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .sellerId(p.getSellerId())
                .imageUrls(copyImageUrls(p.getImageUrls()))
                .build();
    }

    private List<String> copyImageUrls(List<String> imageUrls) {
        return imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    public productRspons UpdateProduct(productRequest productRequest, String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!product.getSellerId().equals(sellerId)) {
            throw ApiException.forbidden("You are not allowed to update this product");
        }
        product.setName(productRequest.getName());
        product.setPrice(productRequest.getPrice());
        product.setDescription(productRequest.getDescription());
        product.setQuantity(productRequest.getQuantity());
        productRepository.save(product);
        return getProduct(product);

    }

    public void DeleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!product.getSellerId().equals(sellerId)) {
            throw ApiException.forbidden("You are not allowed to delete this product");
        }
        productRepository.delete(product);
        productEventProducer.sendProductDeletedEvent(id);
    }

   public FeingResponse getUserProduct(String id, String sellerId) {
    Product product = productRepository.findByIdAndSellerId(id, sellerId)
        .orElseThrow(() -> ApiException.notFound("Product not found or you don't own it"));
    
    return new FeingResponse(product.getId(), product.getName());
}
}
