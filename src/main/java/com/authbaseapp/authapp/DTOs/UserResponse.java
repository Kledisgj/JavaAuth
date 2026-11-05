package com.authbaseapp.authapp.DTOs;


public record UserResponse(
        Long id,
        String name,
        String email,
        String role
) {
}