package com.zerotrust.internalresource.controller;

import com.zerotrust.internalresource.audit.GatewayAccessEvaluator;
import com.zerotrust.internalresource.audit.RequestContextFilter;
import com.zerotrust.internalresource.dto.*;
import com.zerotrust.internalresource.service.DeviceResourceService;
import com.zerotrust.internalresource.service.FileResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ResourceController {
    private final FileResourceService fileService;
    private final DeviceResourceService deviceService;
    private final GatewayAccessEvaluator gatewayAccessEvaluator;

    public ResourceController(FileResourceService fileService, DeviceResourceService deviceService,
                              GatewayAccessEvaluator gatewayAccessEvaluator) {
        this.fileService = fileService;
        this.deviceService = deviceService;
        this.gatewayAccessEvaluator = gatewayAccessEvaluator;
    }

    @GetMapping("/files")
    public ApiResponse<?> listFiles(HttpServletRequest request) { return ApiResponse.success(fileService.findAll(), "files retrieved", trace(request)); }
    @GetMapping("/files/{id}")
    public ApiResponse<?> getFile(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.success(fileService.findById(id), "file retrieved", trace(request)); }
    @PostMapping("/files")
    public ResponseEntity<ApiResponse<?>> createFile(@Valid @RequestBody FileResourceRequest body, HttpServletRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(fileService.create(body), "file created", trace(request))); }
    @PutMapping("/files/{id}")
    public ApiResponse<?> updateFile(@PathVariable Long id, @Valid @RequestBody FileResourceRequest body, HttpServletRequest request) { return ApiResponse.success(fileService.update(id, body), "file updated", trace(request)); }
    @DeleteMapping("/files/{id}")
    public ApiResponse<?> deleteFile(@PathVariable Long id, HttpServletRequest request) { fileService.delete(id); return ApiResponse.success(null, "file deleted", trace(request)); }

    @GetMapping("/devices")
    public ApiResponse<?> listDevices(HttpServletRequest request) { return ApiResponse.success(deviceService.findAll(), "devices retrieved", trace(request)); }
    @GetMapping("/devices/{id}")
    public ApiResponse<?> getDevice(@PathVariable Long id, HttpServletRequest request) { return ApiResponse.success(deviceService.findById(id), "device retrieved", trace(request)); }
    @PostMapping("/devices")
    public ResponseEntity<ApiResponse<?>> createDevice(@Valid @RequestBody DeviceResourceRequest body, HttpServletRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(deviceService.create(body), "device created", trace(request))); }
    @PutMapping("/devices/{id}")
    public ApiResponse<?> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceResourceRequest body, HttpServletRequest request) { return ApiResponse.success(deviceService.update(id, body), "device updated", trace(request)); }
    @DeleteMapping("/devices/{id}")
    public ApiResponse<?> deleteDevice(@PathVariable Long id, HttpServletRequest request) { deviceService.delete(id); return ApiResponse.success(null, "device deleted", trace(request)); }

    @GetMapping("/test-resources/read")
    public ApiResponse<?> readTestResource(HttpServletRequest request) { return test("test-resource-read", "GET", request); }
    @PostMapping("/test-resources/create")
    public ApiResponse<?> createTestResource(HttpServletRequest request) { return test("test-resource-create", "POST", request); }
    @PutMapping("/test-resources/update/{id}")
    public ApiResponse<?> updateTestResource(@PathVariable Long id, HttpServletRequest request) { return test("test-resource-update", "PUT", request); }
    @DeleteMapping("/test-resources/delete/{id}")
    public ApiResponse<?> deleteTestResource(@PathVariable Long id, HttpServletRequest request) { return test("test-resource-delete", "DELETE", request); }

    private ApiResponse<TestResourceResponse> test(String resourceId, String action, HttpServletRequest request) {
        String traceId = trace(request);
        return ApiResponse.success(new TestResourceResponse(resourceId, action,
                "resource accessed through internal-resource-demo", traceId,
                request.getHeader("X-Session-Id") != null,
                gatewayAccessEvaluator.isGatewayAccess(request)), "test resource accessed", traceId);
    }

    private String trace(HttpServletRequest request) { return (String) request.getAttribute(RequestContextFilter.TRACE_ID_ATTRIBUTE); }
}
