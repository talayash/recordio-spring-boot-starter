package com.lognet.recordio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Represents a complete recorded HTTP interaction (request + response).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordEntry {

    private String id;
    private Instant timestamp;
    private String correlationId;
    private long duration;
    private RequestData request;
    private ResponseData response;
    private RecordMetadata metadata;

    public RecordEntry() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public RequestData getRequest() {
        return request;
    }

    public void setRequest(RequestData request) {
        this.request = request;
    }

    public ResponseData getResponse() {
        return response;
    }

    public void setResponse(ResponseData response) {
        this.response = response;
    }

    public RecordMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(RecordMetadata metadata) {
        this.metadata = metadata;
    }
}
