package com.zerotrust.internalresource.dto;
import jakarta.validation.constraints.NotBlank;
public record FileResourceRequest(@NotBlank String name,@NotBlank String description,@NotBlank String category,@NotBlank String owner,@NotBlank String department,@NotBlank String classificationLevel,@NotBlank String status){}
