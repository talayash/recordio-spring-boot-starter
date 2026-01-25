package com.lognet.recordio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Represents the captured HTTP request data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestData {

    private String method;
    private String uri;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Object body;

    public RequestData() {
    }

    public RequestData(String method, String uri, Map<String, String> headers,
                       Map<String, String> queryParams, Object body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.queryParams = queryParams;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
