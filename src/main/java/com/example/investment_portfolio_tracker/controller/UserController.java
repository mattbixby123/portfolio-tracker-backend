package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.UserDto;
import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.model.UserRole;
import com.example.investment_portfolio_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getUserProfile(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return ResponseEntity.ok(convertToDto(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateUserProfile(
            @Valid @RequestBody UserDto userDto,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User updatedUser = userService.updateUser(
                user.getId(),
                userDto.getFirstName(),
                userDto.getLastName(),
                user.getRole() // Keep the existing role - don't allow users to change their own role
        );

        return ResponseEntity.ok(convertToDto(updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // In a real implementation, you would verify the current password
        // before allowing the change, but that's not in your service method

        userService.updatePassword(user.getId(), passwordData.get("newPassword"));
        return ResponseEntity.ok().build();
    }

    // Admin endpoints - should be secured by role-based authorization

    @GetMapping("/admin/all")
    public ResponseEntity<Iterable<UserDto>> getAllUsers(Authentication authentication) {
        // Check if the user is an admin
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                userService.getAllUsers().stream()
                        .map(this::convertToDto)
                        .toList()
        );
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<UserDto> adminUpdateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto,
            Authentication authentication) {

        // Check if the user is an admin
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        User updatedUser = userService.updateUser(
                id,
                userDto.getFirstName(),
                userDto.getLastName(),
                userDto.getRole() // Allow changing roles from admin panel
        );

        return ResponseEntity.ok(convertToDto(updatedUser));
    }

    @PostMapping("/admin/{id}/toggle-enabled")
    public ResponseEntity<Void> toggleUserEnabled(
            @PathVariable Long id,
            Authentication authentication) {

        // Check if the user is an admin
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        userService.toggleUserEnabled(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        // Check if the user is an admin
        User currentUser = userService.getUserByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        // Prevent deleting yourself
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest().build();
        }

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Helper method to convert User to UserDto
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}