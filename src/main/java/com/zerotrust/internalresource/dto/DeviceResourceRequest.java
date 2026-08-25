package com.zerotrust.internalresource.dto;
import jakarta.validation.constraints.NotBlank;
public record DeviceResourceRequest(@NotBlank String deviceName,@NotBlank String ipAddress,@NotBlank String deviceType,@NotBlank String location,@NotBlank String department,@NotBlank String securityLevel,@NotBlank String status){}
