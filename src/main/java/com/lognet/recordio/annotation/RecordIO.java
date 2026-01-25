package com.lognet.recordio.annotation;

import com.lognet.recordio.enums.RecordCondition;
import com.lognet.recordio.enums.RecordFormat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable automatic recording of HTTP request/response data.
 * <p>
 * Can be applied at class level to record all endpoints in a controller,
 * or at method level to record specific endpoints.
 * <p>
 * Method-level annotations override class-level settings.
 *
 * <pre>
 * {@code
 * @RestController
 * @RecordIO
 * public class BookingController {
 *     // All endpoints will be recorded
 * }
 *
 * @RestController
 * public class PaymentController {
 *     @RecordIO(folder = "records/payments", maskFields = {"cardNumber"})
 *     @PostMapping("/payments")
 *     public PaymentResult process(@RequestBody PaymentRequest req) { ... }
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RecordIO {

    /**
     * Enable or disable recording for this controller/method.
     * <p>
     * Default: true
     *
     * @return true if recording is enabled
     */
    boolean enabled() default true;

    /**
     * Output directory path for recorded files.
     * <p>
     * Relative paths are resolved from the application's working directory.
     * <p>
     * Default: "records"
     *
     * @return the output folder path
     */
    String folder() default "";

    /**
     * Output format for the recorded data.
     * <p>
     * Default: JSON
     *
     * @return the output format
     */
    RecordFormat format() default RecordFormat.JSON;

    /**
     * Field names to mask in the recorded output.
     * <p>
     * These fields will have their values replaced with the configured mask value.
     * Common fields to mask: password, token, secret, authorization, cardNumber.
     *
     * @return array of field names to mask
     */
    String[] maskFields() default {};

    /**
     * Whether to include HTTP headers in the recorded output.
     * <p>
     * Default: true
     *
     * @return true if headers should be included
     */
    boolean includeHeaders() default true;

    /**
     * Condition under which to record the request/response.
     * <p>
     * Default: ALWAYS
     *
     * @return the recording condition
     */
    RecordCondition condition() default RecordCondition.ALWAYS;

    /**
     * Whether to format the output with indentation (pretty print).
     * <p>
     * Default: true
     *
     * @return true if output should be pretty printed
     */
    boolean prettyPrint() default true;

    /**
     * Whether to write files asynchronously to minimize latency impact.
     * <p>
     * Default: true
     *
     * @return true if async writing is enabled
     */
    boolean async() default true;

    /**
     * Threshold in milliseconds for slow response recording.
     * <p>
     * Only applicable when condition is set to SLOW_RESPONSE.
     * <p>
     * Default: 1000 (1 second)
     *
     * @return the slow response threshold in milliseconds
     */
    long slowThresholdMs() default 1000L;
}
