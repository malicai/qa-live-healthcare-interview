package com.leansofx.qaserviceuser.dto;

import java.util.Map;

public record ErrorResponse(ErrorDetail error, String timestamp, String requestId) {

    public record ErrorDetail(String code, String message, Map<String, Object> details) {
    }
}
