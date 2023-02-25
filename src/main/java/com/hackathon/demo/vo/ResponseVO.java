package com.hackathon.demo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public class ResponseVO {
    private String statusCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String statusMessage;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> responseData;

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
    
    public Map<String, String> getResponseData() {
        return responseData;
    }

    public void setResponseData(Map<String, String> responseData) {
        this.responseData = responseData;
    }
}

