package com.hubinity.catalog.api.error;

import com.hubinity.catalog.domain.StockMovementType;

/**
 * Thrown when a direct movement request specifies a {@code type} other than {@code IN}/{@code OUT}.
 * {@code RESERVE}/{@code RELEASE}/{@code COMMIT} remain valid journal kinds — written automatically
 * by the reserve/release/commit/expire operations — but are not directly requestable here, since
 * doing so would let a caller move stock between {@code available}/{@code reserved} without going
 * through the atomic reserve/release/commit checks.
 */
public class InvalidStockMovementTypeException extends RuntimeException {

    public InvalidStockMovementTypeException(StockMovementType type) {
        super("Movement type " + type + " is not allowed via this endpoint; only IN or OUT are accepted.");
    }
}
