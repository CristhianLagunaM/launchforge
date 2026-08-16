package com.launchforge.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminUserUpdateRequest(@NotNull Boolean enabled, @NotBlank String role) { }
