package com.azki.reservation.mapper;

import com.azki.reservation.dto.RegisterRequest;
import com.azki.reservation.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String encodedPassword) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        return user;
    }
}