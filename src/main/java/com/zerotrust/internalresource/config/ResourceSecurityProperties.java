package com.zerotrust.internalresource.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "security")
public class ResourceSecurityProperties {
    private boolean directAccessEnabled = true;
    private boolean auditEnabled = true;
    private GatewayProperties gateway = new GatewayProperties();

    public boolean isDirectAccessEnabled() { return directAccessEnabled; }
    public void setDirectAccessEnabled(boolean value) { directAccessEnabled = value; }
    public boolean isAuditEnabled() { return auditEnabled; }
    public void setAuditEnabled(boolean value) { auditEnabled = value; }
    public GatewayProperties getGateway() { return gateway; }
    public void setGateway(GatewayProperties value) { gateway = value; }

    public static class GatewayProperties {
        private String trustMode = "DOCKER_HOST_NAT";
        private String headerName = "X-ZT-Gateway";
        private String headerValue = "zero-trust-rgw";
        private List<String> trustedUpstreamObservedAddresses = new ArrayList<>();

        public String getTrustMode() { return trustMode; }
        public void setTrustMode(String value) { trustMode = value; }
        public String getHeaderName() { return headerName; }
        public void setHeaderName(String value) { headerName = value; }
        public String getHeaderValue() { return headerValue; }
        public void setHeaderValue(String value) { headerValue = value; }
        public List<String> getTrustedUpstreamObservedAddresses() { return trustedUpstreamObservedAddresses; }
        public void setTrustedUpstreamObservedAddresses(List<String> value) { trustedUpstreamObservedAddresses = value; }
    }
}
