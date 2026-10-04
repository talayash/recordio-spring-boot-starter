package com.lognet.recordio.service;

import com.lognet.recordio.config.RecordIOProperties;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingServiceTest {

    private static final String MASK = "***MASKED***";
    private static final String[] NO_EXTRA_FIELDS = {};

    private final MaskingService maskingService = new MaskingService(new RecordIOProperties());

    @Test
    void masksPasswordFieldByNameWithDefaultConfig() {
        Map<String, Object> body = Map.of("username", "alice", "password", "hunter2");

        Object masked = maskingService.maskSensitiveData(body, NO_EXTRA_FIELDS);

        assertThat(masked).isEqualTo(Map.of("username", "alice", "password", MASK));
    }

    @Test
    void masksAuthorizationHeaderWithDefaultConfig() {
        Map<String, String> headers = Map.of("Authorization", "Bearer abc.def", "Accept", "application/json");

        Map<String, String> masked = maskingService.maskHeaders(headers, NO_EXTRA_FIELDS);

        assertThat(masked).containsEntry("Authorization", MASK)
                .containsEntry("Accept", "application/json");
    }

    @Test
    void masksSensitiveFieldsInsidePojo() {
        Object masked = maskingService.maskSensitiveData(new LoginResponse("alice", "jwt-123"), NO_EXTRA_FIELDS);

        assertThat(masked).isEqualTo(Map.of("username", "alice", "accessToken", MASK));
    }

    @Test
    void masksSensitiveFieldsInsideListOfPojos() {
        Object masked = maskingService.maskSensitiveData(List.of(new LoginResponse("bob", "jwt-456")), NO_EXTRA_FIELDS);

        assertThat(masked).isEqualTo(List.of(Map.of("username", "bob", "accessToken", MASK)));
    }

    @Test
    void masksCreditCardNumberInsideStringValueWithDefaultRegex() {
        Object masked = maskingService.maskSensitiveData(Map.of("note", "card 4111111111111111 ok"), NO_EXTRA_FIELDS);

        assertThat(masked).isEqualTo(Map.of("note", "card " + MASK + " ok"));
    }

    @Test
    void masksAnnotationFieldsEvenWhenGlobalMaskingDisabled() {
        RecordIOProperties properties = new RecordIOProperties();
        properties.getMasking().setEnabled(false);
        MaskingService service = new MaskingService(properties);

        Object masked = service.maskSensitiveData(Map.of("ssn", "123", "password", "hunter2"), new String[]{"ssn"});

        assertThat(masked).isEqualTo(Map.of("ssn", MASK, "password", "hunter2"));
    }

    @Test
    void preservesKeyOrder() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("z", 1);
        body.put("a", 2);
        body.put("m", 3);

        Object masked = maskingService.maskSensitiveData(body, NO_EXTRA_FIELDS);

        assertThat(List.copyOf(((Map<?, ?>) masked).keySet())).isEqualTo(List.of("z", "a", "m"));
    }

    record LoginResponse(String username, String accessToken) {
    }
}
