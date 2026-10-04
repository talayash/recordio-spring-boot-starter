package com.lognet.recordio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lognet.recordio.config.RecordIOProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Service for masking sensitive data in recorded request/response bodies and headers.
 */
public class MaskingService {

    private static final Logger logger = LoggerFactory.getLogger(MaskingService.class);

    /**
     * A pattern made only of word characters (e.g. "password") is a field name;
     * anything else (e.g. "\\d{16}") is a regex applied to string values.
     */
    private static final Pattern FIELD_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private final RecordIOProperties properties;
    private final List<Pattern> compiledPatterns;
    private final Set<String> fieldPatterns;
    private final ObjectMapper objectMapper;

    public MaskingService(RecordIOProperties properties) {
        this.properties = properties;
        this.compiledPatterns = new ArrayList<>();
        this.fieldPatterns = new HashSet<>();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        if (properties.getMasking().isEnabled()) {
            for (String pattern : properties.getMasking().getPatterns()) {
                if (FIELD_NAME.matcher(pattern).matches()) {
                    fieldPatterns.add(pattern.toLowerCase());
                    continue;
                }
                try {
                    compiledPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                } catch (PatternSyntaxException e) {
                    logger.warn("Ignoring invalid masking pattern '{}': {}", pattern, e.getMessage());
                }
            }
        }
    }

    /**
     * Masks sensitive data in the given object.
     *
     * @param data            the data to mask
     * @param additionalFields additional field names to mask (from annotation)
     * @return the masked data
     */
    public Object maskSensitiveData(Object data, String[] additionalFields) {
        if (!properties.getMasking().isEnabled() && (additionalFields == null || additionalFields.length == 0)) {
            return data;
        }

        Set<String> allFieldPatterns = new HashSet<>(fieldPatterns);
        if (additionalFields != null) {
            for (String field : additionalFields) {
                allFieldPatterns.add(field.toLowerCase());
            }
        }

        return maskObject(data, allFieldPatterns);
    }

    /**
     * Masks headers (or query parameters) based on configured patterns.
     *
     * @param headers         the headers or query parameters to mask
     * @param additionalFields additional field names to mask
     * @return the masked headers
     */
    public Map<String, String> maskHeaders(Map<String, String> headers, String[] additionalFields) {
        if (headers == null) {
            return null;
        }

        Set<String> allFieldPatterns = new HashSet<>(fieldPatterns);
        if (additionalFields != null) {
            for (String field : additionalFields) {
                allFieldPatterns.add(field.toLowerCase());
            }
        }

        Map<String, String> masked = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (shouldMaskField(key, allFieldPatterns)) {
                masked.put(key, properties.getMasking().getReplacement());
            } else if (value != null) {
                masked.put(key, maskStringValue(value));
            } else {
                masked.put(key, null);
            }
        }

        return masked;
    }

    private Object maskObject(Object obj, Set<String> allFieldPatterns) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Map) {
            return maskMap((Map<?, ?>) obj, allFieldPatterns);
        } else if (obj instanceof Collection) {
            return maskCollection((Collection<?>) obj, allFieldPatterns);
        } else if (obj.getClass().isArray()) {
            return maskArray(obj, allFieldPatterns);
        } else if (obj instanceof String) {
            return maskStringValue((String) obj);
        } else if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character) {
            return obj;
        }

        // POJOs, records, dates, enums: convert to Map/List/String so their fields can be masked
        try {
            return maskObject(objectMapper.convertValue(obj, Object.class), allFieldPatterns);
        } catch (IllegalArgumentException e) {
            logger.debug("Could not convert {} for masking: {}", obj.getClass().getName(), e.getMessage());
            return obj;
        }
    }

    private Map<String, Object> maskMap(Map<?, ?> map, Set<String> allFieldPatterns) {
        Map<String, Object> masked = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (shouldMaskField(key, allFieldPatterns)) {
                masked.put(key, properties.getMasking().getReplacement());
            } else {
                masked.put(key, maskObject(value, allFieldPatterns));
            }
        }

        return masked;
    }

    private List<Object> maskCollection(Collection<?> collection, Set<String> allFieldPatterns) {
        List<Object> masked = new ArrayList<>();
        for (Object item : collection) {
            masked.add(maskObject(item, allFieldPatterns));
        }
        return masked;
    }

    private Object maskArray(Object array, Set<String> allFieldPatterns) {
        if (array instanceof Object[]) {
            Object[] arr = (Object[]) array;
            Object[] masked = new Object[arr.length];
            for (int i = 0; i < arr.length; i++) {
                masked[i] = maskObject(arr[i], allFieldPatterns);
            }
            return Arrays.asList(masked);
        }
        // Primitive arrays are returned as-is
        return array;
    }

    private boolean shouldMaskField(String fieldName, Set<String> allFieldPatterns) {
        if (fieldName == null) {
            return false;
        }

        String lowerFieldName = fieldName.toLowerCase();

        // Check literal field patterns
        if (allFieldPatterns.contains(lowerFieldName)) {
            return true;
        }

        // Check if field name contains any pattern
        for (String pattern : allFieldPatterns) {
            if (lowerFieldName.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private String maskStringValue(String value) {
        if (value == null || compiledPatterns.isEmpty()) {
            return value;
        }

        String result = value;
        for (Pattern pattern : compiledPatterns) {
            result = pattern.matcher(result).replaceAll(properties.getMasking().getReplacement());
        }

        return result;
    }
}
