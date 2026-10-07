package com.learning.miniblog.service;

import com.learning.miniblog.dto.UserRequest;
import com.learning.miniblog.entity.User;
import com.learning.miniblog.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService( UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserRequest request) {

        if(userRepository.existsByEmail(request.getEmail())) {
            
            throw new RuntimeException("Email already exists");

        }

        String encodedPassword = passwordEncoder.encode(request.getPassword()); //// BCrypt Hash

        User user = User.builder()
                        .username( request.getUsername())
                        .email( request.getEmail())
                        .password(encodedPassword)
                        .build();

        userRepository.save(user);
    }

    
}