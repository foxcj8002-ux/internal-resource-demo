package com.zerotrust.internalresource.controller;

import com.zerotrust.internalresource.audit.RequestContextFilter;
import com.zerotrust.internalresource.dto.AccessLogResponse;
import com.zerotrust.internalresource.dto.ApiResponse;
import com.zerotrust.internalresource.service.AccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access-logs")
public class AccessLogController {
    private final AccessLogService service;

    public AccessLogController(AccessLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AccessLogResponse>> findAll(HttpServletRequest request) {
        return ApiResponse.success(service.findAll(), "access logs retrieved", trace(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AccessLogResponse> findById(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(service.findById(id), "access log retrieved", trace(request));
    }

    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(HttpServletRequest request) {
        return ApiResponse.success(Map.of(
                "message", "access log test request reached internal-resource-demo",
                "traceId", trace(request),
                "sessionIdPresent", request.getHeader("X-Session-Id") != null,
                "authorizationPresent", request.getHeader("Authorization") != null),
                "access log test completed", trace(request));
    }

    private String trace(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContextFilter.TRACE_ID_ATTRIBUTE);
    }
}
