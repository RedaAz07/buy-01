package com.microservices.product_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public productRspons AddProduct(productRequest productRequest) {

        Product product = Product
                .builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .quantity(productRequest.getQuantity())
                .sellerId(productRequest.getSellerId())
                .imageUrls(new ArrayList<>(productRequest.getImageUrls()))
                .build();
        productRepository.save(product);
        return getProduct(product);
    }

    public List<productRspons> getall() {
        List<Product> products = productRepository.findAll();
        System.out.println(products);
        return products.stream().map(p -> getProduct(p)).toList();
    }

    public productRspons getProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return getProduct(product);
    }

    private productRspons getProduct(Product p) {
        productRspons product = productRspons.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .sellerId(p.getSellerId())
                .imageUrls(copyImageUrls(p.getImageUrls()))
                .build();
        return product;
    }

    private List<String> copyImageUrls(List<String> imageUrls) {
        return imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    public productRspons UpdateProduct(productRequest productRequest, String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setName(productRequest.getName());
        product.setPrice(productRequest.getPrice());
        product.setDescription(productRequest.getDescription());
        product.setQuantity(productRequest.getQuantity());
        product.setImageUrls(new ArrayList<>(productRequest.getImageUrls()));
        productRepository.save(product);
        return getProduct(product);

    }

    public void DeleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        productRepository.delete(product);
        productEventProducer.sendProductDeletedEvent(id);
    }
}
