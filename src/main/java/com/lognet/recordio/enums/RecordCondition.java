package com.lognet.recordio.enums;

/**
 * Specifies conditions under which request/response data should be recorded.
 */
public enum RecordCondition {
    /**
     * Record every request regardless of response status.
     */
    ALWAYS,

    /**
     * Record only requests that result in 4xx or 5xx responses.
     */
    ON_ERROR,

    /**
     * Record only requests that result in 2xx responses.
     */
    ON_SUCCESS,

    /**
     * Record only requests that exceed the slow response threshold.
     */
    SLOW_RESPONSE,

    /**
     * Never record (same as enabled=false).
     */
    NEVER
}
