package com.authbaseapp.authapp.DTOs;

public record CreateUserRequest(
        String name,
        String email,
        String password,
        String role
) {
}
