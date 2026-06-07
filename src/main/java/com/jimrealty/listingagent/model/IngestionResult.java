package com.jimrealty.listingagent.model;

import java.time.Duration;

/**
 * Captures statistics from a single MLS Grid ingestion pass.
 *
 * Java records are immutable value objects — they get a canonical constructor,
 * equals(), hashCode(), and toString() for free. Perfect for result objects
 * you create once, pass around, and never mutate.
 *
 * toString() output example:
 *   IngestionResult[inserted=142, updated=38, deleted=3, errors=0, duration=PT1M24S]
 */
public record IngestionResult(
        int inserted,
        int updated,
        int deleted,
        int errors,
        Duration duration
) {
    /** Total records touched (does not include errors). */
    public int total() {
        return inserted + updated + deleted;
    }

    /** True if the run completed without any per-record failures. */
    public boolean isClean() {
        return errors == 0;
    }
}