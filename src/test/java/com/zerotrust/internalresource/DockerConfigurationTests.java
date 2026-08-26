package com.zerotrust.internalresource;

import com.zerotrust.internalresource.config.ResourceSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("docker")
class DockerConfigurationTests {
    @Autowired
    private ResourceSecurityProperties securityProperties;

    @Test
    void dockerProfileUsesConfigurableSecurityDefaults() {
        assertThat(securityProperties.isAuditEnabled()).isTrue();
        assertThat(securityProperties.getGateway().getTrustMode()).isEqualTo("DOCKER_HOST_NAT");
        assertThat(securityProperties.getGateway().getTrustedUpstreamObservedAddresses()).contains("172.18.0.1");
    }
}


