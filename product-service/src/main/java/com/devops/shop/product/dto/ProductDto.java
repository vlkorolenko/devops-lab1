package com.devops.shop.product.dto;

import java.math.BigDecimal;

public record ProductDto(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String category,
    Integer stock,
    String imageUrl
) {}
