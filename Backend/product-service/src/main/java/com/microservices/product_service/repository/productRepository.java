package com.microservices.product_service.repository;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.microservices.product_service.model.Product;

/*
save(product);
findAll();
findById(id);
deleteById(id);
delete(product);
existsById(id);
count();
*/
public interface productRepository extends MongoRepository<Product, String> {
      Page<Product> findBySellerId(String sellerId, Pageable pageable);

      boolean existsByIdAndSellerId(String id, String sellerId);
}
