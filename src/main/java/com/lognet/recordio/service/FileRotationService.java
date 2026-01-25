package com.lognet.recordio.service;

import com.lognet.recordio.config.RecordIOProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service for rotating record files based on max count and age.
 */
public class FileRotationService {

    private static final Logger logger = LoggerFactory.getLogger(FileRotationService.class);

    private final RecordIOProperties properties;

    public FileRotationService(RecordIOProperties properties) {
        this.properties = properties;
    }

    /**
     * Rotates files in the given folder for a specific controller/method combination.
     *
     * @param folder     the folder containing record files
     * @param controller the controller name
     * @param method     the method name
     */
    public void rotate(Path folder, String controller, String method) {
        if (!properties.getFileRotation().isEnabled()) {
            return;
        }

        try {
            String prefix = controller + "_" + method + "_";
            rotateByAge(folder);
            rotateByCount(folder, prefix);
        } catch (Exception e) {
            logger.warn("Failed to rotate files in {}: {}", folder, e.getMessage());
        }
    }

    /**
     * Deletes files older than the configured max age.
     *
     * @param folder the folder to clean
     */
    private void rotateByAge(Path folder) {
        if (!Files.exists(folder)) {
            return;
        }

        int maxAgeDays = properties.getFileRotation().getMaxAgeDays();
        Instant cutoff = Instant.now().minus(maxAgeDays, ChronoUnit.DAYS);

        try (Stream<Path> files = Files.list(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> isRecordFile(path))
                    .filter(path -> isOlderThan(path, cutoff))
                    .forEach(this::deleteFile);
        } catch (IOException e) {
            logger.warn("Failed to rotate files by age in {}: {}", folder, e.getMessage());
        }
    }

    /**
     * Deletes excess files beyond the configured max count per endpoint.
     *
     * @param folder the folder to clean
     * @param prefix the file prefix (controller_method_)
     */
    private void rotateByCount(Path folder, String prefix) {
        if (!Files.exists(folder)) {
            return;
        }

        int maxFiles = properties.getFileRotation().getMaxFiles();

        try (Stream<Path> files = Files.list(folder)) {
            List<Path> matchingFiles = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .sorted(Comparator.comparing(this::getLastModifiedTime).reversed())
                    .toList();

            if (matchingFiles.size() > maxFiles) {
                matchingFiles.stream()
                        .skip(maxFiles)
                        .forEach(this::deleteFile);
            }
        } catch (IOException e) {
            logger.warn("Failed to rotate files by count in {}: {}", folder, e.getMessage());
        }
    }

    private boolean isRecordFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".json") || filename.endsWith(".xml");
    }

    private boolean isOlderThan(Path path, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
        } catch (IOException e) {
            return false;
        }
    }

    private Instant getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.MIN;
        }
    }

    private void deleteFile(Path path) {
        try {
            Files.delete(path);
            logger.debug("Deleted rotated file: {}", path);
        } catch (IOException e) {
            logger.warn("Failed to delete file {}: {}", path, e.getMessage());
        }
    }
}
