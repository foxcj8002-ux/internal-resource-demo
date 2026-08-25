package com.zerotrust.internalresource.dto;
public record ErrorResponse(boolean success,String error,String message,String traceId){}
