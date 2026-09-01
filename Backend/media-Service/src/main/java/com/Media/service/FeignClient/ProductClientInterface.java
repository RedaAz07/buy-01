package com.Media.service.FeignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient("PRODUCT-SERVICE")
public interface ProductClientInterface {

    @GetMapping("api/products/owner/{id}")
    public boolean GetUserProduct(@PathVariable String id, @RequestHeader("Authorization") String token);

}