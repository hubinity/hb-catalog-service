package com.hubinity.catalog.api.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception → RFC 7807 {@link ProblemDetail} mapping for the catalog API.
 *
 * <p>Each domain exception gets its own handler returning a {@code type} URI that
 * callers can branch on programmatically, plus a human-readable {@code detail}.
 * See {@code specs/001-category-rest-endpoints/contracts/categories-api.md}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "One or more fields failed validation.");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", fieldErrors(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Category not found");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:category-not-found"));
        return problem;
    }

    @ExceptionHandler(DuplicateSlugException.class)
    public ProblemDetail handleDuplicateSlug(DuplicateSlugException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate slug");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:duplicate-slug"));
        return problem;
    }

    @ExceptionHandler(InvalidParentException.class)
    public ProblemDetail handleInvalidParent(InvalidParentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid parent");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:invalid-parent"));
        return problem;
    }

    @ExceptionHandler(CircularReferenceException.class)
    public ProblemDetail handleCircularReference(CircularReferenceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Circular reference");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:circular-reference"));
        return problem;
    }

    @ExceptionHandler(CategoryHasChildrenException.class)
    public ProblemDetail handleCategoryHasChildren(CategoryHasChildrenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Category has children");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:category-has-children"));
        return problem;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Product not found");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:product-not-found"));
        return problem;
    }

    @ExceptionHandler(DuplicateSkuException.class)
    public ProblemDetail handleDuplicateSku(DuplicateSkuException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate SKU");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:duplicate-sku"));
        return problem;
    }

    @ExceptionHandler(InvalidCategoryException.class)
    public ProblemDetail handleInvalidCategory(InvalidCategoryException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid category");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:invalid-category"));
        return problem;
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid pagination");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:invalid-pagination"));
        return problem;
    }

    @ExceptionHandler(ProductHasStockOrReservationsException.class)
    public ProblemDetail handleProductHasStockOrReservations(ProductHasStockOrReservationsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Product has stock or reservations");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:product-has-stock-or-reservations"));
        return problem;
    }

    @ExceptionHandler(CategoryHasProductsException.class)
    public ProblemDetail handleCategoryHasProducts(CategoryHasProductsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Category has products");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:category-has-products"));
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Insufficient stock");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:insufficient-stock"));
        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Reservation not found");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:reservation-not-found"));
        return problem;
    }

    @ExceptionHandler(ReservationNotActiveException.class)
    public ProblemDetail handleReservationNotActive(ReservationNotActiveException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Reservation not active");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:reservation-not-active"));
        return problem;
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public ProblemDetail handleReservationExpired(ReservationExpiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Reservation expired");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:reservation-expired"));
        return problem;
    }

    @ExceptionHandler(InvalidStockMovementTypeException.class)
    public ProblemDetail handleInvalidStockMovementType(InvalidStockMovementTypeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid stock movement type");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:invalid-stock-movement-type"));
        return problem;
    }

    /**
     * Concurrency safety net: two requests can both pass an {@code existsBySlug}/
     * {@code existsBySku} pre-check before either commits. The DB's partial unique
     * indexes ({@code ux_category_slug_alive}, {@code ux_product_sku_alive}) are the
     * real source of truth — this inspects the violated constraint name to map the
     * failure to the matching duplicate-slug or duplicate-sku response.
     *
     * <p>Any other {@code DataIntegrityViolationException} (a NOT NULL violation, the
     * {@code price_history} FK {@code ON DELETE RESTRICT}, a future check-constraint
     * failure, etc.) does NOT match either known constraint name and must not be
     * mislabeled as a duplicate-slug conflict — it falls through to a generic 500
     * with a non-leaking detail. The raw exception message is logged server-side only.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (cause.contains("ux_product_sku_alive")) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "A product with that SKU already exists.");
            problem.setTitle("Duplicate SKU");
            problem.setType(java.net.URI.create("urn:hubinity:catalog:duplicate-sku"));
            return problem;
        }
        if (cause.contains("ux_category_slug_alive")) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "A category with that slug already exists.");
            problem.setTitle("Duplicate slug");
            problem.setType(java.net.URI.create("urn:hubinity:catalog:duplicate-slug"));
            return problem;
        }
        log.error("Unmapped DataIntegrityViolationException (constraint not recognized): {}", cause, ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "A data integrity constraint was violated.");
        problem.setTitle("Data integrity violation");
        problem.setType(java.net.URI.create("urn:hubinity:catalog:data-integrity-violation"));
        return problem;
    }
}
