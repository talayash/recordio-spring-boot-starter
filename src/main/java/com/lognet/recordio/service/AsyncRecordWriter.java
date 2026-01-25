package com.lognet.recordio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lognet.recordio.config.RecordIOProperties;
import com.lognet.recordio.enums.RecordFormat;
import com.lognet.recordio.model.RecordEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Asynchronous implementation of RecordWriter.
 * <p>
 * Writes record entries to files using CompletableFuture for async processing.
 */
public class AsyncRecordWriter implements RecordWriter {

    private static final Logger logger = LoggerFactory.getLogger(AsyncRecordWriter.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss_SSS")
            .withZone(ZoneId.systemDefault());

    private final RecordIOProperties properties;
    private final Executor executor;
    private final ObjectMapper jsonMapper;
    private final ObjectMapper xmlMapper;
    private final FileRotationService fileRotationService;

    public AsyncRecordWriter(RecordIOProperties properties, Executor executor,
                             FileRotationService fileRotationService) {
        this.properties = properties;
        this.executor = executor;
        this.fileRotationService = fileRotationService;

        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.registerModule(new JavaTimeModule());
        this.jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.xmlMapper = createXmlMapper();
    }

    /**
     * Creates XmlMapper using reflection to avoid hard dependency on jackson-dataformat-xml.
     */
    private ObjectMapper createXmlMapper() {
        try {
            Class<?> xmlMapperClass = Class.forName("com.fasterxml.jackson.dataformat.xml.XmlMapper");
            ObjectMapper mapper = (ObjectMapper) xmlMapperClass.getDeclaredConstructor().newInstance();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        } catch (Exception e) {
            logger.debug("XML support not available - jackson-dataformat-xml not on classpath");
            return null;
        }
    }

    @Override
    public void write(RecordEntry entry, String folder, RecordFormat format, boolean prettyPrint) {
        if (properties.isAsync()) {
            CompletableFuture.runAsync(() -> doWrite(entry, folder, format, prettyPrint), executor)
                    .exceptionally(ex -> {
                        logger.error("Failed to write record asynchronously: {}", ex.getMessage(), ex);
                        return null;
                    });
        } else {
            doWrite(entry, folder, format, prettyPrint);
        }
    }

    private void doWrite(RecordEntry entry, String folder, RecordFormat format, boolean prettyPrint) {
        try {
            // Build folder structure: baseFolder/Controller/method/
            String controller = sanitizeForFilename(entry.getMetadata().getController());
            String method = sanitizeForFilename(entry.getMetadata().getMethod());
            Path basePath = Paths.get(folder, controller, method);

            // Create request and response subfolders
            Path requestFolder = basePath.resolve("request");
            Path responseFolder = basePath.resolve("response");
            Files.createDirectories(requestFolder);
            Files.createDirectories(responseFolder);

            ObjectMapper mapper = getMapper(format);
            if (mapper == null) {
                logger.error("No mapper available for format: {}. Falling back to JSON.", format);
                mapper = jsonMapper;
            }

            String filename = generateFilename(entry, format);

            // Write request file
            if (entry.getRequest() != null) {
                Path requestFile = requestFolder.resolve(filename);
                String requestContent = prettyPrint
                        ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry.getRequest())
                        : mapper.writeValueAsString(entry.getRequest());
                Files.writeString(requestFile, requestContent);
                logger.debug("Request written to: {}", requestFile);
            }

            // Write response file
            if (entry.getResponse() != null) {
                Path responseFile = responseFolder.resolve(filename);
                String responseContent = prettyPrint
                        ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry.getResponse())
                        : mapper.writeValueAsString(entry.getResponse());
                Files.writeString(responseFile, responseContent);
                logger.debug("Response written to: {}", responseFile);
            }

            // Perform file rotation if enabled
            if (fileRotationService != null && properties.getFileRotation().isEnabled()) {
                fileRotationService.rotate(requestFolder, controller, method);
                fileRotationService.rotate(responseFolder, controller, method);
            }

        } catch (IOException e) {
            logger.error("Failed to write record to folder {}: {}", folder, e.getMessage(), e);
        }
    }

    private ObjectMapper getMapper(RecordFormat format) {
        return switch (format) {
            case JSON -> jsonMapper;
            case XML -> xmlMapper;
        };
    }

    private String generateFilename(RecordEntry entry, RecordFormat format) {
        String timestamp = FILENAME_FORMATTER.format(Instant.now());
        String correlationId = entry.getCorrelationId();
        if (correlationId != null && correlationId.length() > 8) {
            correlationId = correlationId.substring(0, 8);
        }

        String extension = format == RecordFormat.XML ? "xml" : "json";
        return String.format("%s_%s.%s", timestamp, correlationId, extension);
    }

    private String sanitizeForFilename(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
