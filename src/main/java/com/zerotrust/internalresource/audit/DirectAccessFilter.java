package com.zerotrust.internalresource.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.internalresource.config.ResourceSecurityProperties;
import com.zerotrust.internalresource.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component("directAccessFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class DirectAccessFilter extends OncePerRequestFilter {
    public static final String AUDIT_RESULT_ATTRIBUTE = DirectAccessFilter.class.getName() + ".auditResult";

    private final ResourceSecurityProperties properties;
    private final GatewayAccessEvaluator gatewayAccessEvaluator;
    private final ObjectMapper objectMapper;

    public DirectAccessFilter(ResourceSecurityProperties properties, GatewayAccessEvaluator gatewayAccessEvaluator,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.gatewayAccessEvaluator = gatewayAccessEvaluator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (properties.isDirectAccessEnabled()
                || !request.getRequestURI().startsWith("/api/")
                || gatewayAccessEvaluator.isGatewayAccess(request)) {
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute(AUDIT_RESULT_ATTRIBUTE, "DIRECT_ACCESS_DISABLED");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String traceId = (String) request.getAttribute(RequestContextFilter.TRACE_ID_ATTRIBUTE);
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(
                false,
                "DIRECT_ACCESS_DISABLED",
                "resource must be accessed through zero-trust-rgw",
                traceId)));
    }
}
