package com.lognet.recordio.aspect;

import com.lognet.recordio.annotation.RecordIO;
import com.lognet.recordio.config.RecordIOProperties;
import com.lognet.recordio.model.RecordEntry;
import com.lognet.recordio.service.MaskingService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordIOAspectMaskingTest {

    private static final String MASK = "***MASKED***";

    private final List<RecordEntry> written = new ArrayList<>();
    private final RecordIOProperties properties = new RecordIOProperties();
    private final RecordIOAspect aspect = new RecordIOAspect(
            properties,
            (entry, folder, format, prettyPrint) -> written.add(entry),
            new MaskingService(properties),
            new MockEnvironment());

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void masksSensitiveQueryParams() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login");
        request.addParameter("token", "abc123");
        request.addParameter("page", "2");

        record(request, null);

        assertThat(written.get(0).getRequest().getQueryParams())
                .containsEntry("token", MASK)
                .containsEntry("page", "2");
    }

    @Test
    void masksSensitiveFieldsInPojoResponseBody() throws Throwable {
        record(new MockHttpServletRequest("POST", "/login"), new LoginResponse("alice", "jwt-123"));

        assertThat(written.get(0).getResponse().getBody())
                .isEqualTo(Map.of("username", "alice", "accessToken", MASK));
    }

    private void record(MockHttpServletRequest request, Object returnValue) throws Throwable {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(request, new MockHttpServletResponse()));

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(TestController.class.getMethod("login"));
        when(signature.getName()).thenReturn("login");

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestController());
        when(joinPoint.proceed()).thenReturn(returnValue);

        aspect.recordIO(joinPoint);
    }

    @RecordIO
    static class TestController {
        public Object login() {
            return null;
        }
    }

    record LoginResponse(String username, String accessToken) {
    }
}
