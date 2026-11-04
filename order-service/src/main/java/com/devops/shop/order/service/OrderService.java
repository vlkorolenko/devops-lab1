package com.devops.shop.order.service;

import com.devops.shop.order.dto.OrderCreateRequest;
import com.devops.shop.order.dto.OrderItemDto;
import com.devops.shop.order.dto.OrderResponseDto;
import com.devops.shop.order.model.Order;
import com.devops.shop.order.model.OrderItem;
import com.devops.shop.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponseDto> getOrderById(Long id) {
        return orderRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequest request) {
        Order order = new Order(
                request.customerName(),
                request.customerEmail(),
                request.customerAddress()
        );

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemDto itemDto : request.items()) {
            OrderItem item = new OrderItem(
                    itemDto.productId(),
                    itemDto.productName(),
                    itemDto.price(),
                    itemDto.quantity()
            );
            order.addItem(item);

            BigDecimal itemTotal = itemDto.price().multiply(BigDecimal.valueOf(itemDto.quantity()));
            total = total.add(itemTotal);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        return toDto(saved);
    }

    public OrderResponseDto toDto(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getProductId(),
                        i.getProductName(),
                        i.getPrice(),
                        i.getQuantity()
                ))
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerAddress(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                items
        );
    }
}
