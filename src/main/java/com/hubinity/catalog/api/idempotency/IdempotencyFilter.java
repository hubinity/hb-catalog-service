package com.hubinity.catalog.api.idempotency;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import tools.jackson.databind.ObjectMapper;

import com.hubinity.catalog.domain.IdempotencyRecord;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enforces the mandatory {@code Idempotency-Key} header on the four mutating stock endpoints
 * (FR-014–016, FR-021), via a claim-row pattern that also closes a race a naive
 * "check-then-act" filter would miss: two truly simultaneous retries with the same key must
 * never both execute the underlying business logic (SC-002).
 *
 * <p>Registered (not as an auto-detected {@code @Component} — see {@code IdempotencyFilterConfig})
 * only on the four mutating stock paths; the read-only movement-history endpoint is untouched.
 *
 * <p>Runs <em>before</em> {@code DispatcherServlet}, so none of the three error responses this
 * filter produces (missing header, conflict, in-progress) can flow through
 * {@code ApiExceptionHandler}'s {@code @RestControllerAdvice} — they are hand-built
 * {@link ProblemDetail} JSON written directly to the response. See
 * specs/003-stock-movement-reservation/research.md ("Idempotency storage").
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final long RETENTION_HOURS = 24;

    /**
     * Exact patterns this filter applies to. Registered broadly via
     * {@code FilterRegistrationBean} ({@code IdempotencyFilterConfig}) since standard
     * servlet-mapping syntax has no equivalent of a wildcard segment mid-path
     * (e.g. {@code /products/*}/stock/movements} is not valid there) — {@link AntPathMatcher}
     * does the real narrowing here, in {@link #shouldNotFilter}.
     */
    private static final List<String> MUTATING_PATTERNS = List.of(
            "/api/v1/products/*/stock/movements",
            "/api/v1/stock/reservations",
            "/api/v1/stock/reservations/*/release",
            "/api/v1/stock/reservations/*/commit");

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        return MUTATING_PATTERNS.stream().noneMatch(pattern -> PATH_MATCHER.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            writeProblem(response, HttpStatus.BAD_REQUEST, "Idempotency key missing",
                    "An Idempotency-Key header is required for this request.", "idempotency-key-missing");
            return;
        }

        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
        String requestHash = hash(request.getMethod() + " " + request.getRequestURI() + " "
                + new String(wrappedRequest.bodyBytes(), StandardCharsets.UTF_8));

        if (idempotencyService.claim(key, requestHash) == 1) {
            proceedAndRecord(key, requestHash, wrappedRequest, response, chain);
            return;
        }

        Optional<IdempotencyRecord> existing = idempotencyService.findById(key);
        if (existing.isEmpty()) {
            // Claimed-then-released (5xx) between our claim attempt and this read — retry once.
            if (idempotencyService.claim(key, requestHash) == 1) {
                proceedAndRecord(key, requestHash, wrappedRequest, response, chain);
            } else {
                writeProblem(response, HttpStatus.CONFLICT, "Idempotency key in progress",
                        "Another request with this Idempotency-Key is still being processed; retry shortly.",
                        "idempotency-key-in-progress");
            }
            return;
        }

        IdempotencyRecord record = existing.get();
        if (record.getResponseStatus() == 0) {
            writeProblem(response, HttpStatus.CONFLICT, "Idempotency key in progress",
                    "Another request with this Idempotency-Key is still being processed; retry shortly.",
                    "idempotency-key-in-progress");
            return;
        }

        boolean stale = record.getCreatedAt().isBefore(Instant.now().minus(RETENTION_HOURS, ChronoUnit.HOURS));
        if (!stale && record.getRequestHash().equals(requestHash)) {
            replay(response, record);
            return;
        }
        if (!stale) {
            writeProblem(response, HttpStatus.CONFLICT, "Idempotency key conflict",
                    "This Idempotency-Key was already used for a request with different input.",
                    "idempotency-key-conflict");
            return;
        }

        // Stale (>24h) — treated as expired per the platform's idempotency policy: re-claim
        // afresh by overwriting the row with a new "pending" placeholder, then proceed as new.
        idempotencyService.finalizeRecord(key, requestHash, 0, "");
        proceedAndRecord(key, requestHash, wrappedRequest, response, chain);
    }

    private void proceedAndRecord(
            String key, String requestHash, CachedBodyRequestWrapper wrappedRequest,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            int status = wrappedResponse.getStatus();
            if (status >= 500) {
                idempotencyService.releaseClaim(key);
            } else {
                String body = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                idempotencyService.finalizeRecord(key, requestHash, status, body);
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        response.setContentType("application/json");
        response.getWriter().write(record.getResponseBody());
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String title, String detail, String urnSlug)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(java.net.URI.create("urn:hubinity:catalog:" + urnSlug));
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Buffers the entire request body once in the constructor so it can be read twice: once by
     * this filter (for hashing) and again, normally, by Spring's {@code HttpMessageConverter}
     * downstream. {@code ContentCachingRequestWrapper} alone does not support this — its cache
     * only reflects bytes read by the <em>first</em> consumer, so a second {@code getInputStream()}
     * call would see an already-exhausted underlying stream.
     */
    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        byte[] bodyBytes() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return byteStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }
}
