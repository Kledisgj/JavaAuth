package com.authbaseapp.authapp.Controller;

import jakarta.validation.Valid;

import com.authbaseapp.authapp.DTOs.*;
import com.authbaseapp.authapp.Entity.UsersApp;
import com.authbaseapp.authapp.Service.UserAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAppService userAppService;
    private final JwtEncoder jwtEncoder;

    public AuthController(UserAppService userAppService, JwtEncoder jwtEncoder) {
        this.userAppService = userAppService;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registered successfully", userAppService.register(request)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UsersApp user = userAppService.login(request);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .claim("id", user.getId())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ApiResponse.ok("Logged in successfully", new AuthResponse(token, user.getRole()));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(
                "Current user",
                userAppService.currentUser(jwt.getSubject()));
    }
}






