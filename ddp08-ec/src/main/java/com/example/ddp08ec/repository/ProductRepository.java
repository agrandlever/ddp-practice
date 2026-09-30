package com.example.ddp08ec.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ddp08ec.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
}
