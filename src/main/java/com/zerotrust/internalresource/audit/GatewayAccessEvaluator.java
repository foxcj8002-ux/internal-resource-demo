package com.zerotrust.internalresource.audit;

import com.zerotrust.internalresource.config.ResourceSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class GatewayAccessEvaluator {
    private static final String DOCKER_HOST_NAT = "DOCKER_HOST_NAT";
    private final ResourceSecurityProperties properties;

    public GatewayAccessEvaluator(ResourceSecurityProperties properties) {
        this.properties = properties;
    }

    public boolean isGatewayAccess(HttpServletRequest request) {
        ResourceSecurityProperties.GatewayProperties gateway = properties.getGateway();
        boolean headerMatches = gateway.getHeaderValue().equals(request.getHeader(gateway.getHeaderName()));
        if (!headerMatches) {
            return false;
        }
        if (DOCKER_HOST_NAT.equalsIgnoreCase(gateway.getTrustMode())) {
            return gateway.getTrustedUpstreamObservedAddresses().contains(request.getRemoteAddr());
        }
        return false;
    }
}
