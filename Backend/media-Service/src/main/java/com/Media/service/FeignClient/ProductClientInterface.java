package com.Media.service.FeignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.Media.dto.request.ProductDTO;

@FeignClient("PRODUCT-SERVICE")
public interface ProductClientInterface {

    @GetMapping("api/products/owner/{id}")
    public ProductDTO GetUserProduct(@PathVariable String id, @RequestHeader("Authorization") String token);

}