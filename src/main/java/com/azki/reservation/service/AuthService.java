package com.azki.reservation.service;

import com.azki.reservation.dto.LoginRequest;
import com.azki.reservation.dto.RegisterRequest;
import com.azki.reservation.entity.User;
import com.azki.reservation.exception.DuplicateUserException;
import com.azki.reservation.mapper.UserMapper;
import com.azki.reservation.repository.UserRepository;
import com.azki.reservation.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        userRepository.save(userMapper.toEntity(request, encodedPassword));
    }

    public String login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return tokenProvider.generateToken(user.getUsername(), user.getId());
    }
}