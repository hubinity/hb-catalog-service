/**
 * HTTP adapter layer: {@code @RestControllerAdvice} that maps domain exceptions
 * ({@code com.hubinity.catalog.domain.error}) to RFC 7807 {@code ProblemDetail} responses,
 * and HTTP-specific validation exceptions like {@code InvalidPaginationException}.
 */
package com.hubinity.catalog.api.error;
