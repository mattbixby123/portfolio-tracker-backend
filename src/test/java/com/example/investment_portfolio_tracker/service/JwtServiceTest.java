package com.example.investment_portfolio_tracker.service;

import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Set required properties via reflection (simulates @Value annotation)
        ReflectionTestUtils.setField(jwtService, "secretKey", "testsecrettestsecrettestsecrettestsecrettestsecrettestsecret");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
    }

    @Test
    void shouldGenerateToken() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void shouldGenerateTokenWithExtraClaims() {
        // Given
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "USER");
        extraClaims.put("firstName", "Test");

        // When
        String token = jwtService.generateToken(extraClaims, testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Verify claims - use a different approach to avoid ambiguity
        String role = jwtService.extractClaim(token, claims -> claims.get("role").toString());
        String firstName = jwtService.extractClaim(token, claims -> claims.get("firstName").toString());

        assertThat(role).isEqualTo("USER");
        assertThat(firstName).isEqualTo("Test");
    }

    @Test
    void shouldExtractUsername() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void shouldExtractExpiration() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        Date expirationDate = jwtService.extractExpiration(token);

        // Then
        assertThat(expirationDate).isAfter(new Date());
    }

    @Test
    void shouldCheckIfTokenIsValid() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldCheckIfTokenIsInvalidForDifferentUser() {
        // Given
        String token = jwtService.generateToken(testUser);

        User differentUser = new User();
        differentUser.setEmail("different@example.com");

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }
}