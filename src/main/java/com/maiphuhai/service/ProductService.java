package com.maiphuhai.service;

import com.maiphuhai.model.Product;
import com.maiphuhai.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByAscPrice() {
        return productRepository.findAllSortedByAcs();
    }

    public List<Product> findByDescPrice() {
        return productRepository.findAllSortedByDesc();
    }
}
