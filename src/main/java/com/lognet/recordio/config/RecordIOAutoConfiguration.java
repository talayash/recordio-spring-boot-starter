package com.lognet.recordio.config;

import com.lognet.recordio.aspect.RecordIOAspect;
import com.lognet.recordio.filter.RecordIOFilter;
import com.lognet.recordio.service.AsyncRecordWriter;
import com.lognet.recordio.service.FileRotationService;
import com.lognet.recordio.service.MaskingService;
import com.lognet.recordio.service.RecordWriter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Auto-configuration for RecordIO.
 * <p>
 * This configuration is automatically applied when the starter is on the classpath
 * and can be disabled by setting {@code recordio.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "recordio", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RecordIOProperties.class)
public class RecordIOAutoConfiguration {

    /**
     * Creates the executor for async file writing.
     */
    @Bean
    @ConditionalOnMissingBean(name = "recordIOExecutor")
    public Executor recordIOExecutor() {
        return Executors.newCachedThreadPool();
    }

    /**
     * Creates the masking service for sensitive data.
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskingService maskingService(RecordIOProperties properties) {
        return new MaskingService(properties);
    }

    /**
     * Creates the file rotation service.
     */
    @Bean
    @ConditionalOnMissingBean
    public FileRotationService fileRotationService(RecordIOProperties properties) {
        return new FileRotationService(properties);
    }

    /**
     * Creates the record writer for persisting records.
     */
    @Bean
    @ConditionalOnMissingBean
    public RecordWriter recordWriter(RecordIOProperties properties, Executor recordIOExecutor,
                                      FileRotationService fileRotationService) {
        return new AsyncRecordWriter(properties, recordIOExecutor, fileRotationService);
    }

    /**
     * Creates the AOP aspect for intercepting @RecordIO annotated methods.
     */
    @Bean
    @ConditionalOnMissingBean
    public RecordIOAspect recordIOAspect(RecordIOProperties properties, RecordWriter recordWriter,
                                          MaskingService maskingService, Environment environment) {
        return new RecordIOAspect(properties, recordWriter, maskingService, environment);
    }

    /**
     * Registers the filter for request/response body caching.
     */
    @Bean
    @ConditionalOnMissingBean(name = "recordIOFilterRegistration")
    public FilterRegistrationBean<RecordIOFilter> recordIOFilterRegistration() {
        FilterRegistrationBean<RecordIOFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RecordIOFilter());
        registration.addUrlPatterns("/*");
        registration.setName("recordIOFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
