package com.launchforge.admin.api;

import com.launchforge.admin.api.dto.AdminUserResponse;
import com.launchforge.admin.api.dto.AdminUserUpdateRequest;
import com.launchforge.admin.application.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }
    @GetMapping public List<AdminUserResponse> list() { return service.list(); }
    @PatchMapping("/{id}") public ResponseEntity<AdminUserResponse> update(@PathVariable UUID id, @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
