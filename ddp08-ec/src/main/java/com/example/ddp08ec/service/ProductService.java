package com.example.ddp08ec.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ddp08ec.entity.Product;
import com.example.ddp08ec.form.ProductForm;
import com.example.ddp08ec.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product create(ProductForm productForm) {
        Product product = new Product();
        copyFormToProduct(productForm, product);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public Optional<Product> update(Long id, ProductForm productForm) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isEmpty()) {
            return Optional.empty();
        }

        Product product = existingProduct.get();
        // 既存の商品を更新し、登録日時はそのまま保持する。
        copyFormToProduct(productForm, product);
        product.setUpdatedAt(LocalDateTime.now());
        return Optional.of(productRepository.save(product));
    }

    @Transactional
    public boolean delete(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            return false;
        }

        productRepository.delete(product.get());
        return true;
    }

    private void copyFormToProduct(ProductForm productForm, Product product) {
        product.setName(productForm.getName());
        product.setPrice(productForm.getPrice());
        product.setDescription(productForm.getDescription());
        product.setCategory(productForm.getCategory());
        product.setOnSale(productForm.getOnSale());
    }
}
