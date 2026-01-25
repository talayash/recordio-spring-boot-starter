package com.lognet.recordio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Represents the captured HTTP response data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseData {

    private int status;
    private Map<String, String> headers;
    private Object body;

    public ResponseData() {
    }

    public ResponseData(int status, Map<String, String> headers, Object body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
