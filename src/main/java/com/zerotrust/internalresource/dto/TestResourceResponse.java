package com.zerotrust.internalresource.dto;
public record TestResourceResponse(String resourceId,String action,String message,String traceId,boolean sessionIdPresent,boolean gatewayAccess){}
