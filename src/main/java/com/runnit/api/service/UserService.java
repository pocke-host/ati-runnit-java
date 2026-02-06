package com.runnit.api.service;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Create a new user (signup)
    public User createUser(String email, String password, String fullName) {
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, hashedPassword, fullName);
        return userRepository.save(user);
    }

    // Fetch user by email (used in login or profile lookup)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Fetch all users (for admin dashboards, etc.)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Update user profile
    public User updateUser(Long id, User updatedUser) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
        existing.setFullName(updatedUser.getFullName());
        existing.setBio(updatedUser.getBio());
        existing.setProfilePictureUrl(updatedUser.getProfilePictureUrl());
        return userRepository.save(existing);
    }

    // Delete user
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
