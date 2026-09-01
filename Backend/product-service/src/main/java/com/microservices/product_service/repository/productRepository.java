package com.microservices.product_service.repository;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.Optional;

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
      List<Product> findBySellerId(String sellerId);

      Optional<Product> findByIdAndSellerId(String id, String sellerId);
}
