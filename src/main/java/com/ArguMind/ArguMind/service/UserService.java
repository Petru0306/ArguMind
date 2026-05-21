package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
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

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("User already exists!");
        }

        User user = User.builder()
                .username(registrationDto.getUsername())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
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
