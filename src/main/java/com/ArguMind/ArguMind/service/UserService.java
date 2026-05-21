package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.ProfileUpdateDto;
import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
import com.ArguMind.ArguMind.exception.EmailAlreadyExistsException;
import com.ArguMind.ArguMind.exception.UsernameAlreadyExistsException;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RankService rankService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    @Transactional
    public UserRegistrationResponseDto registerUser(UserRegistrationDto registrationDto) {
        String username = normalize(registrationDto.getUsername());
        String email = normalizeEmail(registrationDto.getEmail());

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (registrationDto.getPassword() == null || registrationDto.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (registrationDto.getConfirmPassword() != null
                && !registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        return UserRegistrationResponseDto.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .eloRating(savedUser.getEloRating())
                .rankTitle(savedUser.getRankTitle())
                .build();
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String email = normalizeEmail(dto.getEmail());
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email-ul este obligatoriu.");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Format email invalid.");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new EmailAlreadyExistsException(email);
        }

        user.setEmail(email);
        user.setRankTitle(rankService.titleForElo(user.getEloRating()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserRegistrationResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserRegistrationResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .eloRating(user.getEloRating())
                .rankTitle(user.getRankTitle())
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
