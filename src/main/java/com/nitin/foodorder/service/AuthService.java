package com.nitin.foodorder.service;

import com.nitin.foodorder.dto.AuthDtos.AuthResponse;
import com.nitin.foodorder.dto.AuthDtos.LoginRequest;
import com.nitin.foodorder.dto.AuthDtos.RegisterRequest;
import com.nitin.foodorder.entity.Role;
import com.nitin.foodorder.entity.User;
import com.nitin.foodorder.exception.BadRequestException;
import com.nitin.foodorder.exception.ConflictException;
import com.nitin.foodorder.repository.UserRepository;
import com.nitin.foodorder.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role == Role.ADMIN) {
            throw new BadRequestException("ADMIN accounts cannot be self-registered");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), user.getEmail(), user.getRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
