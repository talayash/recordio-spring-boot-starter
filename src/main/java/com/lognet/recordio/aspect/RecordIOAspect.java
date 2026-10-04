package com.lognet.recordio.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lognet.recordio.annotation.RecordIO;
import com.lognet.recordio.config.RecordIOProperties;
import com.lognet.recordio.enums.RecordCondition;
import com.lognet.recordio.enums.RecordFormat;
import com.lognet.recordio.model.RecordEntry;
import com.lognet.recordio.model.RecordMetadata;
import com.lognet.recordio.model.RequestData;
import com.lognet.recordio.model.ResponseData;
import com.lognet.recordio.service.MaskingService;
import com.lognet.recordio.service.RecordWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AOP Aspect that intercepts controller methods annotated with @RecordIO
 * and records HTTP request/response data.
 */
@Aspect
public class RecordIOAspect {

    private static final Logger logger = LoggerFactory.getLogger(RecordIOAspect.class);

    private final RecordIOProperties properties;
    private final RecordWriter recordWriter;
    private final MaskingService maskingService;
    private final Environment environment;
    private final ObjectMapper objectMapper;

    public RecordIOAspect(RecordIOProperties properties, RecordWriter recordWriter,
                          MaskingService maskingService, Environment environment) {
        this.properties = properties;
        this.recordWriter = recordWriter;
        this.maskingService = maskingService;
        this.environment = environment;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Intercepts methods annotated with @RecordIO or methods in classes annotated with @RecordIO.
     */
    @Around("@annotation(com.lognet.recordio.annotation.RecordIO) || " +
            "@within(com.lognet.recordio.annotation.RecordIO)")
    public Object recordIO(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check if globally enabled
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // Check profile restrictions
        if (!isActiveForCurrentProfile()) {
            return joinPoint.proceed();
        }

        // Get annotation (method-level takes precedence over class-level)
        RecordIO annotation = getAnnotation(joinPoint);
        if (annotation == null || !annotation.enabled() || annotation.condition() == RecordCondition.NEVER) {
            return joinPoint.proceed();
        }

        // Get HTTP request/response
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.debug("No request context available, skipping recording");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        if (response == null) {
            logger.debug("No response available, skipping recording");
            return joinPoint.proceed();
        }

        // Generate correlation ID
        String correlationId = UUID.randomUUID().toString().replace("-", "");
        Instant timestamp = Instant.now();
        long startTime = System.currentTimeMillis();

        // Proceed with the actual method execution
        Object result = null;
        Throwable thrownException = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            thrownException = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = thrownException != null ? 500 : response.getStatus();

            // Check if we should record based on condition
            if (shouldRecord(annotation, statusCode, duration)) {
                try {
                    RecordEntry entry = buildRecordEntry(
                            joinPoint, annotation, request, response, result,
                            correlationId, timestamp, duration, statusCode
                    );

                    String folder = resolveFolder(annotation);
                    RecordFormat format = resolveFormat(annotation);
                    boolean prettyPrint = resolvePrettyPrint(annotation);

                    recordWriter.write(entry, folder, format, prettyPrint);
                } catch (Exception e) {
                    logger.error("Failed to record request/response: {}", e.getMessage(), e);
                }
            }
        }
    }

    private RecordIO getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Method-level annotation takes precedence
        RecordIO methodAnnotation = method.getAnnotation(RecordIO.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        // Fall back to class-level annotation
        return joinPoint.getTarget().getClass().getAnnotation(RecordIO.class);
    }

    private boolean isActiveForCurrentProfile() {
        if (properties.getProfiles().isEmpty()) {
            return true;
        }

        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            activeProfiles = environment.getDefaultProfiles();
        }

        for (String activeProfile : activeProfiles) {
            if (properties.getProfiles().contains(activeProfile)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldRecord(RecordIO annotation, int statusCode, long duration) {
        return switch (annotation.condition()) {
            case ALWAYS -> true;
            case ON_ERROR -> statusCode >= 400;
            case ON_SUCCESS -> statusCode >= 200 && statusCode < 300;
            case SLOW_RESPONSE -> duration >= annotation.slowThresholdMs();
            case NEVER -> false;
        };
    }

    private RecordEntry buildRecordEntry(ProceedingJoinPoint joinPoint, RecordIO annotation,
                                          HttpServletRequest request, HttpServletResponse response, Object result,
                                          String correlationId, Instant timestamp, long duration, int statusCode) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();

        RecordEntry entry = new RecordEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTimestamp(timestamp);
        entry.setCorrelationId(correlationId);
        entry.setDuration(duration);

        // Build request data
        entry.setRequest(buildRequestData(request, annotation));

        // Build response data - pass the result object to capture the response body
        entry.setResponse(buildResponseData(response, statusCode, result, annotation));

        // Build metadata
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "default";
        entry.setMetadata(new RecordMetadata(controllerName, methodName, profile));

        return entry;
    }

    private RequestData buildRequestData(HttpServletRequest request, RecordIO annotation) {
        RequestData data = new RequestData();
        data.setMethod(request.getMethod());
        data.setUri(request.getRequestURI());

        // Include headers if configured
        boolean includeHeaders = annotation.includeHeaders() && properties.isIncludeHeaders();
        if (includeHeaders) {
            Map<String, String> headers = extractHeaders(request);
            data.setHeaders(maskingService.maskHeaders(headers, annotation.maskFields()));
        }

        // Query parameters
        Map<String, String> queryParams = extractQueryParams(request);
        if (!queryParams.isEmpty()) {
            data.setQueryParams(maskingService.maskHeaders(queryParams, annotation.maskFields()));
        }

        // Request body
        Object body = extractRequestBody(request);
        if (body != null) {
            data.setBody(maskingService.maskSensitiveData(body, annotation.maskFields()));
        }

        return data;
    }

    private ResponseData buildResponseData(HttpServletResponse response, int statusCode, Object result, RecordIO annotation) {
        ResponseData data = new ResponseData();
        data.setStatus(statusCode);

        // Include headers if configured
        boolean includeHeaders = annotation.includeHeaders() && properties.isIncludeHeaders();
        if (includeHeaders) {
            Map<String, String> headers = extractResponseHeaders(response);
            data.setHeaders(maskingService.maskHeaders(headers, annotation.maskFields()));
        }

        // Response body - use the method return value directly
        Object body = result;

        // If no result, try to extract from response wrapper (fallback)
        if (body == null) {
            body = extractResponseBody(response);
        }

        if (body != null) {
            data.setBody(maskingService.maskSensitiveData(body, annotation.maskFields()));
        }

        return data;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private Map<String, String> extractResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, response.getHeader(name));
        }
        return headers;
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values.length == 1 ? values[0] : Arrays.toString(values));
            }
        });
        return params;
    }

    private Object extractRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return parseBody(content);
            }
        }
        return null;
    }

    private Object extractResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                return parseBody(content);
            }
        }
        return null;
    }

    private Object parseBody(byte[] content) {
        // Check max body size
        long maxSize = properties.getMaxBodySize().toBytes();
        if (content.length > maxSize) {
            return "[Body truncated - exceeded " + properties.getMaxBodySize() + "]";
        }

        String bodyString = new String(content, StandardCharsets.UTF_8);

        // Try to parse as JSON
        try {
            return objectMapper.readValue(bodyString, Object.class);
        } catch (JsonProcessingException e) {
            // Return as plain string if not valid JSON
            return bodyString;
        }
    }

    private String resolveFolder(RecordIO annotation) {
        String folder = annotation.folder();
        if (folder == null || folder.isEmpty()) {
            return properties.getBaseFolder();
        }
        return folder;
    }

    private RecordFormat resolveFormat(RecordIO annotation) {
        // Use annotation format if not default, otherwise use properties
        return annotation.format();
    }

    private boolean resolvePrettyPrint(RecordIO annotation) {
        return annotation.prettyPrint() && properties.isPrettyPrint();
    }
}
