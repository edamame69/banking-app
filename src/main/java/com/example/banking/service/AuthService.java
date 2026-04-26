package com.example.banking.service;

import com.example.banking.domain.User;
import com.example.banking.dto.AuthResponse;
import com.example.banking.dto.LoginRequest;
import com.example.banking.dto.RegisterRequest;
import com.example.banking.exception.InvalidPasswordException;
import com.example.banking.exception.UserAlreadyExistsException;
import com.example.banking.exception.UserNotFoundException;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        var saved = userRepository.save(user);
        var token = jwtUtils.generateToken(saved);
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.email());

        if (existingUser.isEmpty()) {
            throw new UserNotFoundException(request.email());
        }

        if (!passwordEncoder.matches(request.password(), existingUser.get().getPassword())) {
            throw new InvalidPasswordException();
        }

        User user = existingUser.get();
        var token = jwtUtils.generateToken(user);
        return new AuthResponse(token);
    }
}
