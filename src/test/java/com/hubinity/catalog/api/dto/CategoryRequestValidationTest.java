package com.hubinity.catalog.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("CategoryRequest Bean Validation")
class CategoryRequestValidationTest {

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

    private CategoryRequest valid() {
        return new CategoryRequest("Electronics", "electronics", null, null, null);
    }

    @Test
    @DisplayName("valid payload has no violations")
    void valid_payload_passes() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    @DisplayName("blank name is rejected")
    void blank_name_rejected() {
        CategoryRequest req = new CategoryRequest("", "electronics", null, null, null);
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("blank slug is rejected")
    void blank_slug_rejected() {
        CategoryRequest req = new CategoryRequest("Electronics", "", null, null, null);
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slug"));
    }

    @Test
    @DisplayName("name longer than 120 characters is rejected")
    void name_too_long_rejected() {
        CategoryRequest req = new CategoryRequest("a".repeat(121), "electronics", null, null, null);
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("name with exactly 120 characters is accepted")
    void name_at_max_length_accepted() {
        CategoryRequest req = new CategoryRequest("a".repeat(120), "electronics", null, null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    @DisplayName("slug longer than 150 characters is rejected")
    void slug_too_long_rejected() {
        String tooLong = "a".repeat(151);
        CategoryRequest req = new CategoryRequest("Electronics", tooLong, null, null, null);
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slug"));
    }

    @ParameterizedTest
    @DisplayName("malformed slug is rejected")
    @ValueSource(strings = {
            "Electronics",        // uppercase
            "-electronics",       // leading hyphen
            "electronics-",       // trailing hyphen
            "electronics--phones",// double hyphen
            "electronics phones", // space
            "electronics_phones", // underscore
    })
    void malformed_slug_rejected(String slug) {
        CategoryRequest req = new CategoryRequest("Electronics", slug, null, null, null);
        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("slug"));
    }

    @ParameterizedTest
    @DisplayName("well-formed slugs are accepted")
    @ValueSource(strings = {"electronics", "electronics-phones", "a1-b2-c3", "123"})
    void wellformed_slug_accepted(String slug) {
        CategoryRequest req = new CategoryRequest("Electronics", slug, null, null, null);
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    @DisplayName("optional fields (parentId, displayOrder, active) may be omitted")
    void optional_fields_may_be_null() {
        CategoryRequest req = new CategoryRequest("Electronics", "electronics", (UUID) null, null, null);
        assertThat(validator.validate(req)).isEmpty();
    }
}
