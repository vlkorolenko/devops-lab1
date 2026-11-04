package com.devops.shop.product.service;

import com.devops.shop.product.dto.ProductCreateRequest;
import com.devops.shop.product.dto.ProductDto;
import com.devops.shop.product.model.Product;
import com.devops.shop.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts(String category, String search) {
        List<Product> products;
        if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryIgnoreCase(category.trim());
        } else if (search != null && !search.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search.trim(), search.trim());
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional
    public ProductDto createProduct(ProductCreateRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.stock(),
                request.imageUrl()
        );
        Product saved = productRepository.save(product);
        return toDto(saved);
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory(),
                p.getStock(),
                p.getImageUrl()
        );
    }
}
