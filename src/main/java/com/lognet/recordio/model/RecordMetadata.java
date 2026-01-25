package com.lognet.recordio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metadata about the recorded request/response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordMetadata {

    private String controller;
    private String method;
    private String profile;

    public RecordMetadata() {
    }

    public RecordMetadata(String controller, String method, String profile) {
        this.controller = controller;
        this.method = method;
        this.profile = profile;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
