package com.devops.shop.order;

import com.devops.shop.order.dto.OrderCreateRequest;
import com.devops.shop.order.dto.OrderItemDto;
import com.devops.shop.order.dto.OrderResponseDto;
import com.devops.shop.order.model.Order;
import com.devops.shop.order.model.OrderItem;
import com.devops.shop.order.repository.OrderRepository;
import com.devops.shop.order.service.OrderService;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Unit: Розрахунок загальної вартості замовлення (price * quantity)")
    void testCreateOrderCalculatesTotalCorrectly() {
        OrderItemDto item1 = new OrderItemDto(1L, "Товар 1", new BigDecimal("100.00"), 2); // 200.00
        OrderItemDto item2 = new OrderItemDto(2L, "Товар 2", new BigDecimal("49.99"), 3);  // 149.97
        // Total should be 349.97

        OrderCreateRequest request = new OrderCreateRequest(
                "Іван Коваленко",
                "ivan@example.com",
                "вул. Хрещатик 1, Київ",
                List.of(item1, item2)
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(10L);
            return savedOrder;
        });

        OrderResponseDto result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.customerName()).isEqualTo("Іван Коваленко");
        assertThat(result.totalAmount()).isEqualByComparingTo("349.97");
        assertThat(result.items()).hasSize(2);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("Unit: Отримання списку замовлень")
    void testGetAllOrders() {
        Order order = new Order("Олена", "olena@example.com", "Львів");
        order.setId(1L);
        order.setTotalAmount(new BigDecimal("150.00"));
        order.addItem(new OrderItem(1L, "Ноутбук", new BigDecimal("150.00"), 1));

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<OrderResponseDto> orders = orderService.getAllOrders();

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).customerName()).isEqualTo("Олена");
        verify(orderRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Unit: Отримання замовлення за ID")
    void testGetOrderById() {
        Order order = new Order("Тарас", "taras@example.com", "Одеса");
        order.setId(5L);
        order.setTotalAmount(new BigDecimal("80.00"));

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        Optional<OrderResponseDto> result = orderService.getOrderById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().customerEmail()).isEqualTo("taras@example.com");
    }
}
