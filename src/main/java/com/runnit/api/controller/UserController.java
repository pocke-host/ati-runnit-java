package com.runnit.api.controller;

import com.runnit.api.model.User;
import com.runnit.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create new user (signup)
    @PostMapping("/signup")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(
            user.getEmail(),
            user.getPasswordHash(), // in practice, rename this field to "password" in your DTO
            user.getFullName()
        );
        return ResponseEntity.ok(created);
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get one user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.getUserByEmail("user@example.com") // or findById in repo
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Update profile
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        return ResponseEntity.ok(userService.updateUser(id, updatedUser));
    }

    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
