package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserRegistrationResponseDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("User already exists!");
        }

        User user = User.builder()
                .username(registrationDto.getUsername())
                .password(registrationDto.getPassword()) // TODO: BCrypt
                .build();

        User savedUser = userRepository.save(user);

        return UserRegistrationResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .eloRating(savedUser.getEloRating())
                .rankTitle(savedUser.getRankTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public UserRegistrationResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserRegistrationResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .eloRating(user.getEloRating())
                .rankTitle(user.getRankTitle())
                .build();
    }
}
