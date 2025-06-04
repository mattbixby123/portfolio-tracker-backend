package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.AuthenticationRequest;
import com.example.investment_portfolio_tracker.dto.AuthenticationResponse;
import com.example.investment_portfolio_tracker.dto.RegisterRequest;
import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.model.UserRole;
import com.example.investment_portfolio_tracker.service.JwtService;
import com.example.investment_portfolio_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.secret}")
    private String adminSecret;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName(),
                UserRole.USER
        );

        String token = jwtService.generateToken(user);

        // Create response with all parameters
        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setTokenType("Bearer");
        response.setRole(user.getRole().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) userService.getUserByEmail(request.getEmail()).orElseThrow();
        String token = jwtService.generateToken(user);

        // Create response with all parameters
        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setTokenType("Bearer");
        response.setRole(user.getRole().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/toggle-role")
    public ResponseEntity<AuthenticationResponse> toggleUserRole(
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Admin-Secret") String providedSecret) {

        // Check if the admin secret matches your configured value
        if (!adminSecret.equals(providedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get the email from the request body
        String email = requestBody.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Get the user
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Toggle the role
        UserRole newRole = (user.getRole() == UserRole.ADMIN) ? UserRole.USER : UserRole.ADMIN;

        // Update the user
        User updatedUser = userService.updateUser(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                newRole
        );

        // Generate a new token with the updated role
        String token = jwtService.generateToken(updatedUser);

        // Create response
        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(token);
        response.setEmail(updatedUser.getEmail());
        response.setFirstName(updatedUser.getFirstName());
        response.setLastName(updatedUser.getLastName());
        response.setTokenType("Bearer");
        response.setRole(user.getRole().name());

        return ResponseEntity.ok(response);
    }
}