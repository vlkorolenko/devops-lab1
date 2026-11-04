package com.devops.shop.product.config;

import com.devops.shop.product.model.Product;
import com.devops.shop.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                List<Product> demoProducts = List.of(
                        new Product(
                                "Ноутбук Pro Developer 16\"",
                                "Потужний ноутбук для розробки та DevOps завдань: 32GB RAM, 1TB SSD, 12-core CPU.",
                                new BigDecimal("1899.99"),
                                "Laptops",
                                15,
                                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500&auto=format&fit=crop&q=60"
                        ),
                        new Product(
                                "Механічна клавіатура RGB",
                                "Бездротова механічна клавіатура з перемикачами Hot-Swap та ергономічним дизайном.",
                                new BigDecimal("129.50"),
                                "Accessories",
                                42,
                                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&auto=format&fit=crop&q=60"
                        ),
                        new Product(
                                "Бездротова миша Precision",
                                "Високоточна оптична миша 4000 DPI з підключенням Bluetooth та USB Type-C зарядкою.",
                                new BigDecimal("69.99"),
                                "Accessories",
                                50,
                                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=500&auto=format&fit=crop&q=60"
                        ),
                        new Product(
                                "4K UltraWide Монітор 34\"",
                                "Вигнутий IPS-монітор 144Hz з підтримкою HDR400 та вбудованим USB-хабом на 90W.",
                                new BigDecimal("649.00"),
                                "Monitors",
                                8,
                                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&auto=format&fit=crop&q=60"
                        ),
                        new Product(
                                "Бездротові навушники з ANC",
                                "Преміальні навушники з активним шумозаглушенням та автономністю до 35 годин.",
                                new BigDecimal("249.99"),
                                "Audio",
                                20,
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60"
                        ),
                        new Product(
                                "USB-C Док-станція 10-в-1",
                                "Універсальний хаб з портами HDMI 4K, Gigabit Ethernet, SD Card та 100W Power Delivery.",
                                new BigDecimal("79.90"),
                                "Accessories",
                                35,
                                "https://images.unsplash.com/photo-1616401784845-180882ba9ba8?w=500&auto=format&fit=crop&q=60"
                        )
                );
                repository.saveAll(demoProducts);
            }
        };
    }
}
