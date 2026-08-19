package com.microservices.product_service.repository;

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

}
