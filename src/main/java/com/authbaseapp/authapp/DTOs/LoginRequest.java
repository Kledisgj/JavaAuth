package com.authbaseapp.authapp.DTOs;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {

    // BCrypt accepts at most 72 bytes, which can be fewer than 72 characters.
    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
