package com.devops.shop.product;

import com.devops.shop.product.dto.ProductCreateRequest;
import com.devops.shop.product.dto.ProductDto;
import com.devops.shop.product.model.Product;
import com.devops.shop.product.repository.ProductRepository;
import com.devops.shop.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product(
                1L,
                "Тестовий монітор",
                "Опис",
                new BigDecimal("299.99"),
                "Monitors",
                10,
                "http://example.com/img.jpg"
        );
    }

    @Test
    @DisplayName("Unit: Отримання списку всіх товарів")
    void testGetAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        List<ProductDto> result = productService.getAllProducts(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Тестовий монітор");
        assertThat(result.get(0).price()).isEqualByComparingTo("299.99");
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Unit: Фільтрація товарів за категорією")
    void testGetProductsByCategory() {
        when(productRepository.findByCategoryIgnoreCase("monitors")).thenReturn(List.of(testProduct));

        List<ProductDto> result = productService.getAllProducts("monitors", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Monitors");
        verify(productRepository, times(1)).findByCategoryIgnoreCase("monitors");
    }

    @Test
    @DisplayName("Unit: Отримання товару за існуючим ID")
    void testGetProductByIdFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Optional<ProductDto> result = productService.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Unit: Отримання товару за неіснуючим ID")
    void testGetProductByIdNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<ProductDto> result = productService.getProductById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Unit: Створення нового товару")
    void testCreateProduct() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Новий товар",
                "Опис нового товару",
                new BigDecimal("99.50"),
                "Accessories",
                25,
                "http://example.com/item.jpg"
        );

        when(productRepository.save(any(Product.class))).thenReturn(
                new Product(2L, request.name(), request.description(), request.price(), request.category(), request.stock(), request.imageUrl())
        );

        ProductDto created = productService.createProduct(request);

        assertThat(created).isNotNull();
        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.name()).isEqualTo("Новий товар");
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
