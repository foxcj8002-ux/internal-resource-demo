package com.zerotrust.internalresource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.internalresource.repository.AccessLogRepository;
import com.zerotrust.internalresource.repository.DeviceResourceRepository;
import com.zerotrust.internalresource.repository.FileResourceRepository;
import com.zerotrust.internalresource.config.ResourceSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternalResourceDemoApplicationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired FileResourceRepository fileRepository;
    @Autowired DeviceResourceRepository deviceRepository;
    @Autowired AccessLogRepository accessLogRepository;
    @Autowired ResourceSecurityProperties securityProperties;

    @AfterEach
    void resetSecurityConfiguration() {
        securityProperties.setDirectAccessEnabled(true);
        securityProperties.getGateway().setTrustMode("DOCKER_HOST_NAT");
        securityProperties.getGateway().setHeaderName("X-ZT-Gateway");
        securityProperties.getGateway().setHeaderValue("zero-trust-rgw");
        securityProperties.getGateway().setTrustedUpstreamObservedAddresses(List.of("127.0.0.1"));
    }

    @Test
    void initializedResourcesAreAvailable() throws Exception {
        assertThat(fileRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(deviceRepository.count()).isGreaterThanOrEqualTo(5);
        mockMvc.perform(get("/api/files")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(5));
        mockMvc.perform(get("/api/devices")).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    void fileCrudUsesServerControlledResourceId() throws Exception {
        String createBody = json(Map.of("name", "Created file", "description", "description", "category", "report",
                "owner", "tester", "department", "security", "classificationLevel", "INTERNAL", "status", "ACTIVE",
                "resourceId", "client-forged-resource"));
        String created = mockMvc.perform(post("/api/files").contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.resourceId").value("internal-files"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("id").asLong();
        mockMvc.perform(get("/api/files/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Created file"));
        String updateBody = json(Map.of("name", "Updated file", "description", "updated", "category", "configuration",
                "owner", "tester", "department", "operations", "classificationLevel", "CONFIDENTIAL", "status", "ACTIVE"));
        mockMvc.perform(put("/api/files/{id}", id).contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Updated file"))
                .andExpect(jsonPath("$.data.resourceId").value("internal-files"));
        mockMvc.perform(delete("/api/files/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/files/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deviceCrudUsesServerControlledResourceId() throws Exception {
        String body = json(Map.of("deviceName", "Created device", "ipAddress", "192.168.20.10", "deviceType", "simulator",
                "location", "lab", "department", "operations", "securityLevel", "HIGH", "status", "ONLINE"));
        String created = mockMvc.perform(post("/api/devices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.resourceId").value("internal-devices"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("id").asLong();
        mockMvc.perform(get("/api/devices/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(put("/api/devices/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.resourceId").value("internal-devices"));
        mockMvc.perform(delete("/api/devices/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/devices/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void missingResourceReturns404WithTraceId() throws Exception {
        mockMvc.perform(get("/api/files/999999").header("X-Trace-Id", "trace-not-found"))
                .andExpect(status().isNotFound()).andExpect(header().string("X-Trace-Id", "trace-not-found"))
                .andExpect(jsonPath("$.traceId").value("trace-not-found"));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        mockMvc.perform(post("/api/files").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void traceIdIsGeneratedAndReturnedInJson() throws Exception {
        mockMvc.perform(get("/api/test-resources/read"))
                .andExpect(status().isOk()).andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.traceId").isNotEmpty()).andExpect(jsonPath("$.data.traceId").isNotEmpty());
    }

    @Test
    void existingTraceIdIsPassedThrough() throws Exception {
        mockMvc.perform(get("/api/test-resources/read").header("X-Trace-Id", "trace-passthrough"))
                .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id", "trace-passthrough"))
                .andExpect(jsonPath("$.traceId").value("trace-passthrough"))
                .andExpect(jsonPath("$.data.traceId").value("trace-passthrough"));
    }

    @Test
    void trustedRemoteIpAndGatewayHeaderAreBothRequired() throws Exception {
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(true));
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("10.10.10.10"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw").header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(false));
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("127.0.0.1"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(false));
    }

    @Test
    void dockerHostNatUsesObservedAddressAndNeverForwardedFor() throws Exception {
        securityProperties.getGateway().setTrustMode("DOCKER_HOST_NAT");
        securityProperties.getGateway().setTrustedUpstreamObservedAddresses(java.util.List.of("172.18.0.1"));
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("172.18.0.1"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw").header("X-Forwarded-For", "192.168.0.111"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(true));
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("10.0.0.20"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw").header("X-Forwarded-For", "172.18.0.1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(false));
        var log = accessLogRepository.findAll().stream().filter(item -> "172.18.0.1".equals(item.getActualRemoteAddr())).findFirst().orElseThrow();
        assertThat(log.getActualRemoteAddr()).isEqualTo("172.18.0.1");
        assertThat(log.getForwardedFor()).isEqualTo("192.168.0.111");
        assertThat(log.isGatewayAccess()).isTrue();
    }
    @Test
    void allTestResourceActionsUseActualHttpMethods() throws Exception {
        mockMvc.perform(post("/api/test-resources/create")).andExpect(status().isOk()).andExpect(jsonPath("$.data.resourceId").value("test-resource-create")).andExpect(jsonPath("$.data.action").value("POST"));
        mockMvc.perform(put("/api/test-resources/update/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.resourceId").value("test-resource-update")).andExpect(jsonPath("$.data.action").value("PUT"));
        mockMvc.perform(delete("/api/test-resources/delete/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.resourceId").value("test-resource-delete")).andExpect(jsonPath("$.data.action").value("DELETE"));
    }

    @Test
    void accessLogProvesRequestReachedResourceWithoutStoringAuthorization() throws Exception {
        long before = accessLogRepository.count();
        mockMvc.perform(get("/api/test-resources/read").header("X-Trace-Id", "audit-trace")
                        .header("Authorization", "Bearer secret-token").header("X-Session-Id", "session-secret"))
                .andExpect(status().isOk());
        assertThat(accessLogRepository.count()).isEqualTo(before + 1);
        var log = accessLogRepository.findAll().stream().filter(item -> "audit-trace".equals(item.getTraceId())).findFirst().orElseThrow();
        assertThat(log.isAuthorizationPresent()).isTrue();
        assertThat(log.isSessionIdPresent()).isTrue();
        assertThat(log.getResourceId()).isEqualTo("test-resource-read");
        assertThat(log.getHttpMethod()).isEqualTo("GET");
    }

    @Test
    void accessLogQueryAndSingleQueryReturnSafeAuditFields() throws Exception {
        String traceId = "access-log-query-trace";
        mockMvc.perform(post("/api/access-logs/test").header("X-Trace-Id", traceId)
                        .header("Authorization", "Bearer should-not-be-stored")
                        .header("X-Session-Id", "session-should-not-be-stored"))
                .andExpect(status().isOk());
        var log = accessLogRepository.findAll().stream().filter(item -> traceId.equals(item.getTraceId())).findFirst().orElseThrow();
        mockMvc.perform(get("/api/access-logs").header("X-Trace-Id", "query-trace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.traceId").value("query-trace"))
                .andExpect(jsonPath("$.data[?(@.id == " + log.getId() + ")].traceId").value("access-log-query-trace"));
        mockMvc.perform(get("/api/access-logs/{id}", log.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(log.getId().intValue()))
                .andExpect(jsonPath("$.data.authorizationPresent").value(true))
                .andExpect(jsonPath("$.data.sessionIdPresent").value(true))
                .andExpect(jsonPath("$.data.authorization").doesNotExist())
                .andExpect(jsonPath("$.data.sessionId").doesNotExist());
    }

    @Test
    void directAccessEnabledAllowsDirectResourceRequest() throws Exception {
        securityProperties.setDirectAccessEnabled(true);
        mockMvc.perform(get("/api/test-resources/read"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(false));
    }

    @Test
    void directAccessDisabledRejectsBeforeBusinessLogicAndKeepsTrace() throws Exception {
        securityProperties.setDirectAccessEnabled(false);
        long filesBefore = fileRepository.count();
        String body = json(Map.of("name", "must not create", "description", "description", "category", "report",
                "owner", "tester", "department", "security", "classificationLevel", "INTERNAL", "status", "ACTIVE"));
        mockMvc.perform(post("/api/files").header("X-Trace-Id", "direct-disabled-trace")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(header().string("X-Trace-Id", "direct-disabled-trace"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("DIRECT_ACCESS_DISABLED"))
                .andExpect(jsonPath("$.traceId").value("direct-disabled-trace"));
        assertThat(fileRepository.count()).isEqualTo(filesBefore);
        securityProperties.setDirectAccessEnabled(true);
        var log = accessLogRepository.findAll().stream().filter(item -> "direct-disabled-trace".equals(item.getTraceId())).findFirst().orElseThrow();
        assertThat(log.getResult()).isEqualTo("DIRECT_ACCESS_DISABLED");
    }

    @Test
    void directAccessDisabledAllowsTrustedGatewayRequest() throws Exception {
        securityProperties.setDirectAccessEnabled(false);
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("127.0.0.1"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.gatewayAccess").value(true));
    }

    @Test
    void directAccessDisabledRejectsUntrustedGatewayHeaderAndForwardedForCannotForgeIt() throws Exception {
        securityProperties.setDirectAccessEnabled(false);
        mockMvc.perform(get("/api/test-resources/read").with(request -> { request.setRemoteAddr("10.10.10.10"); return request; })
                        .header("X-ZT-Gateway", "zero-trust-rgw").header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("DIRECT_ACCESS_DISABLED"));
    }

    @Test
    void authorizationAndSessionValuesAreNotPersisted() throws Exception {
        String traceId = "sensitive-values-trace";
        mockMvc.perform(get("/api/test-resources/read").header("X-Trace-Id", traceId)
                        .header("Authorization", "Bearer top-secret-jwt")
                        .header("X-Session-Id", "top-secret-session"))
                .andExpect(status().isOk());
        var log = accessLogRepository.findAll().stream().filter(item -> traceId.equals(item.getTraceId())).findFirst().orElseThrow();
        assertThat(log.isAuthorizationPresent()).isTrue();
        assertThat(log.isSessionIdPresent()).isTrue();
        String serialized = objectMapper.writeValueAsString(log);
        assertThat(serialized).doesNotContain("top-secret-jwt").doesNotContain("top-secret-session");
    }
    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }
}

