package com.hubinity.catalog.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("ProductRequest Bean Validation")
class ProductRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private ProductRequest valid() {
        return new ProductRequest("SKU-001", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
    }

    @Test
    @DisplayName("valid payload has no violations")
    void valid_payload_passes() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("blank sku is rejected")
    void blank_sku_rejected() {
        ProductRequest req = new ProductRequest("", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sku"));
    }

    @Test
    @DisplayName("blank name is rejected")
    void blank_name_rejected() {
        ProductRequest req = new ProductRequest("SKU-001", "", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("sku longer than 64 characters is rejected")
    void sku_too_long_rejected() {
        ProductRequest req = new ProductRequest("a".repeat(65), "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sku"));
    }

    @Test
    @DisplayName("name longer than 200 characters is rejected")
    void name_too_long_rejected() {
        ProductRequest req = new ProductRequest("SKU-001", "a".repeat(201), null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("price of exactly zero is rejected")
    void zero_price_rejected() {
        ProductRequest req = new ProductRequest("SKU-001", "Widget", null, new BigDecimal("0.00"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }

    @Test
    @DisplayName("negative price is rejected")
    void negative_price_rejected() {
        ProductRequest req = new ProductRequest("SKU-001", "Widget", null, new BigDecimal("-1.00"), null, UUID.randomUUID(), null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("price"));
    }

    @Test
    @DisplayName("price of 0.01 is accepted")
    void smallest_positive_price_accepted() {
        ProductRequest req = new ProductRequest("SKU-001", "Widget", null, new BigDecimal("0.01"), null, UUID.randomUUID(), null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    @DisplayName("missing categoryId is rejected")
    void missing_categoryId_rejected() {
        ProductRequest req = new ProductRequest("SKU-001", "Widget", null, new BigDecimal("9.90"), null, null, null, null);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("categoryId"));
    }

    @Test
    @DisplayName("optional fields (description, costPrice, active, barcode) may be omitted")
    void optional_fields_may_be_null() {
        ProductRequest req = new ProductRequest("SKU-001", "Widget", null, new BigDecimal("9.90"), null, UUID.randomUUID(), null, null);
        assertThat(validator.validate(req)).isEmpty();
    }
}
