package com.hubinity.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Hubinity HB Catalog Service.
 *
 * <p>This service is the source of truth for HiBit products, categories and
 * stock. It exposes a REST API contract (see {@code contracts-catalog}) and
 * publishes domain events to the {@code catalog.events} RabbitMQ exchange
 * (see {@code contracts-events}).
 *
 * <p>Bootstrap-only at present (Phase 1, feature 1.1) — business code lands
 * in subsequent features 1.3–1.8.
 *
 * <p>{@code @EnableScheduling} backs {@code StockService.sweepExpiredReservations()}
 * (feature 003-stock-movement-reservation) — the only {@code @Scheduled} job
 * in this service.
 */
@SpringBootApplication
@EnableScheduling
public class HbCatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HbCatalogServiceApplication.class, args);
    }
}
