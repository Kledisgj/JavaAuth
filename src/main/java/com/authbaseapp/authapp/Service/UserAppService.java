package com.authbaseapp.authapp.Service;

import com.authbaseapp.authapp.DTOs.LoginRequest;
import com.authbaseapp.authapp.DTOs.RegisterRequest;
import com.authbaseapp.authapp.DTOs.UserResponse;
import com.authbaseapp.authapp.Entity.UsersApp;
import com.authbaseapp.authapp.Repository.UsersAppRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class UserAppService {
    private final UsersAppRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAppService(UsersAppRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        UsersApp user = new UsersApp();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        return toResponse(userRepository.save(user));
    }

    public UsersApp login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UsersApp user = userRepository.findByEmail(email)
                .filter(found -> !found.isDelete())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return user;
    }

    public UserResponse currentUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> !user.isDelete())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is unavailable"));
    }

    private UserResponse toResponse(UsersApp user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
