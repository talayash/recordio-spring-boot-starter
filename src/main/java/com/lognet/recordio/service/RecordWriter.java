package com.lognet.recordio.service;

import com.lognet.recordio.enums.RecordFormat;
import com.lognet.recordio.model.RecordEntry;

/**
 * Interface for writing record entries to persistent storage.
 */
public interface RecordWriter {

    /**
     * Writes a record entry to the specified folder.
     *
     * @param entry       the record entry to write
     * @param folder      the output folder path
     * @param format      the output format (JSON or XML)
     * @param prettyPrint whether to format output with indentation
     */
    void write(RecordEntry entry, String folder, RecordFormat format, boolean prettyPrint);
}
