package com.zerotrust.internalresource.controller;

import com.zerotrust.internalresource.audit.GatewayAccessEvaluator;
import com.zerotrust.internalresource.audit.RequestContextFilter;
import com.zerotrust.internalresource.config.ResourceSecurityProperties;
import com.zerotrust.internalresource.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final ResourceSecurityProperties properties;
    private final GatewayAccessEvaluator gatewayAccessEvaluator;
    private final Environment environment;

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${application.version}")
    private String applicationVersion;

    public SystemController(ResourceSecurityProperties properties, GatewayAccessEvaluator gatewayAccessEvaluator,
                            Environment environment) {
        this.properties = properties;
        this.gatewayAccessEvaluator = gatewayAccessEvaluator;
        this.environment = environment;
    }

    @GetMapping("/info")
    public ApiResponse<?> info(HttpServletRequest request) {
        String[] profiles = environment.getActiveProfiles();
        return ApiResponse.success(Map.of(
                "applicationName", applicationName,
                "version", applicationVersion,
                "environment", profiles.length == 0 ? "default" : String.join(",", profiles),
                "currentTime", Instant.now(),
                "serverPort", request.getLocalPort(),
                "directAccessEnabled", properties.isDirectAccessEnabled()), "system information", trace(request));
    }

    @GetMapping("/network-info")
    public ApiResponse<?> networkInfo(HttpServletRequest request) {
        return ApiResponse.success(Map.of(
                "clientIp", request.getRemoteAddr(),
                "forwardedFor", header(request, "X-Forwarded-For"),
                "forwardedProto", header(request, "X-Forwarded-Proto"),
                "userAgent", header(request, "User-Agent"),
                "traceId", trace(request),
                "sessionIdPresent", request.getHeader("X-Session-Id") != null,
                "gatewayAccess", gatewayAccessEvaluator.isGatewayAccess(request)), "network information", trace(request));
    }

    private String trace(HttpServletRequest request) { return (String) request.getAttribute(RequestContextFilter.TRACE_ID_ATTRIBUTE); }
    private String header(HttpServletRequest request, String name) { String value = request.getHeader(name); return value == null ? "" : value; }
}
