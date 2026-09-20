package com.shoppingApp.productservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.shoppingApp.productservice.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
}
