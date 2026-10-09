package com.prakashbankers.controller;

import com.prakashbankers.dto.ApiDtos.ChangePasswordRequest;
import com.prakashbankers.dto.ApiDtos.LoginRequest;
import com.prakashbankers.dto.ApiDtos.LoginResponse;
import com.prakashbankers.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
