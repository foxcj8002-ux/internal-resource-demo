package com.zerotrust.internalresource.dto;

import com.zerotrust.internalresource.entity.AccessLogEntity;

import java.time.Instant;

public record AccessLogResponse(Long id, String traceId, String requestId, String requestPath,
                                String httpMethod, String clientIp, String forwardedFor, String userAgent,
                                boolean sessionIdPresent, boolean deviceFingerprintPresent,
                                boolean authorizationPresent, String resourceId, String result,
                                boolean gatewayAccess, Integer responseStatus, Long processingTime,
                                Instant createdAt) {
    public static AccessLogResponse from(AccessLogEntity entity) {
        return new AccessLogResponse(entity.getId(), entity.getTraceId(), entity.getRequestId(),
                entity.getRequestPath(), entity.getHttpMethod(), entity.getClientIp(), entity.getForwardedFor(),
                entity.getUserAgent(), entity.isSessionIdPresent(), entity.isDeviceFingerprintPresent(),
                entity.isAuthorizationPresent(), entity.getResourceId(), entity.getResult(),
                entity.isGatewayAccess(), entity.getResponseStatus(), entity.getProcessingTime(), entity.getCreatedAt());
    }
}
