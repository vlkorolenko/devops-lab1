package com.devops.shop.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderCreateRequest(
    @NotBlank(message = "Ім'я замовника обов'язкове")
    @Size(min = 2, max = 100, message = "Ім'я має містити від 2 до 100 символів")
    String customerName,

    @NotBlank(message = "Email обов'язковий")
    @Email(message = "Вкажіть коректний email")
    String customerEmail,

    @NotBlank(message = "Адреса доставки обов'язкова")
    @Size(min = 5, max = 255, message = "Адреса має містити від 5 до 255 символів")
    String customerAddress,

    @NotEmpty(message = "Замовлення має містити хоча б один товар")
    @Valid
    List<OrderItemDto> items
) {}
