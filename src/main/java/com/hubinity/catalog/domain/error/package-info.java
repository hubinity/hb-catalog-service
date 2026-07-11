/**
 * Domain-level exceptions — business rule violations and invariant checks.
 *
 * <p>These are thrown deep in the service layer when a business rule is violated. The HTTP adapter
 * layer ({@code api.error.ApiExceptionHandler}) maps each to an RFC 7807 ProblemDetail response.
 * Moving them here (from {@code api.error}) ensures the domain can be tested and deployed
 * independently of any HTTP concern.
 */
package com.hubinity.catalog.domain.error;
