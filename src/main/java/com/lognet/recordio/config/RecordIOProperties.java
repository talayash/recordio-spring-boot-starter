package com.lognet.recordio.config;

import com.lognet.recordio.enums.RecordFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for RecordIO.
 * <p>
 * Can be configured via application.yml or application.properties:
 * <pre>
 * recordio:
 *   enabled: true
 *   base-folder: "records"
 *   format: JSON
 *   pretty-print: true
 *   async: true
 *   include-headers: true
 *   max-body-size: 10KB
 *   file-rotation:
 *     enabled: true
 *     max-files: 100
 *     max-age-days: 7
 *   masking:
 *     enabled: true
 *     patterns:
 *       - "password"
 *       - "token"
 *     replacement: "***MASKED***"
 *   profiles:
 *     - dev
 *     - local
 * </pre>
 */
@ConfigurationProperties(prefix = "recordio")
public class RecordIOProperties {

    /**
     * Master switch to enable/disable RecordIO globally.
     */
    private boolean enabled = true;

    /**
     * Default output folder for recorded files.
     */
    private String baseFolder = "records";

    /**
     * Default output format (JSON or XML).
     */
    private RecordFormat format = RecordFormat.JSON;

    /**
     * Whether to format output with indentation.
     */
    private boolean prettyPrint = true;

    /**
     * Whether to write files asynchronously.
     */
    private boolean async = true;

    /**
     * Whether to include HTTP headers in recordings.
     */
    private boolean includeHeaders = true;

    /**
     * Maximum body size to record. Larger bodies will be truncated.
     */
    @DataSizeUnit(DataUnit.KILOBYTES)
    private DataSize maxBodySize = DataSize.ofKilobytes(10);

    /**
     * File rotation settings.
     */
    private FileRotation fileRotation = new FileRotation();

    /**
     * Masking settings for sensitive data.
     */
    private Masking masking = new Masking();

    /**
     * Spring profiles in which RecordIO is active.
     * If empty, RecordIO is active in all profiles.
     */
    private List<String> profiles = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    public void setBaseFolder(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    public RecordFormat getFormat() {
        return format;
    }

    public void setFormat(RecordFormat format) {
        this.format = format;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean isIncludeHeaders() {
        return includeHeaders;
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    public DataSize getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(DataSize maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public FileRotation getFileRotation() {
        return fileRotation;
    }

    public void setFileRotation(FileRotation fileRotation) {
        this.fileRotation = fileRotation;
    }

    public Masking getMasking() {
        return masking;
    }

    public void setMasking(Masking masking) {
        this.masking = masking;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles;
    }

    /**
     * File rotation configuration.
     */
    public static class FileRotation {

        /**
         * Whether file rotation is enabled.
         */
        private boolean enabled = true;

        /**
         * Maximum number of files to keep per endpoint.
         */
        private int maxFiles = 100;

        /**
         * Maximum age in days before files are deleted.
         */
        private int maxAgeDays = 7;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxFiles() {
            return maxFiles;
        }

        public void setMaxFiles(int maxFiles) {
            this.maxFiles = maxFiles;
        }

        public int getMaxAgeDays() {
            return maxAgeDays;
        }

        public void setMaxAgeDays(int maxAgeDays) {
            this.maxAgeDays = maxAgeDays;
        }
    }

    /**
     * Masking configuration for sensitive data.
     */
    public static class Masking {

        /**
         * Whether masking is enabled.
         */
        private boolean enabled = true;

        /**
         * Patterns (field names or regex) to mask.
         */
        private List<String> patterns = List.of(
                "password",
                "token",
                "secret",
                "authorization",
                "\\d{16}" // Credit card numbers
        );

        /**
         * Replacement string for masked values.
         */
        private String replacement = "***MASKED***";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getPatterns() {
            return patterns;
        }

        public void setPatterns(List<String> patterns) {
            this.patterns = patterns;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }
}
