package com.example.investment_portfolio_tracker.service;

import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.model.UserRole;
import com.example.investment_portfolio_tracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // When
        List<User> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getId()).isEqualTo(1L);
        assertThat(users.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void shouldGetUserById() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldGetUserByEmail() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void shouldCreateUser() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded_password");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User createdUser = userService.createUser(
                "test@example.com",
                "password",
                "John",
                "Doe",
                UserRole.USER
        );

        // Then
        assertThat(createdUser.getId()).isEqualTo(1L);
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedUser.getPassword()).isEqualTo("encoded_password");
        assertThat(capturedUser.getFirstName()).isEqualTo("John");
        assertThat(capturedUser.getLastName()).isEqualTo("Doe");
        assertThat(capturedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(capturedUser.isEnabled()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenEmailExistsOnCreate() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() ->
                userService.createUser(
                        "test@example.com",
                        "password",
                        "John",
                        "Doe",
                        UserRole.USER
                )
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldUpdateUser() {
        // Given
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setRole(UserRole.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // When
        User updatedUser = userService.updateUser(1L, "Jane", "Smith", UserRole.ADMIN);

        // Then
        verify(userRepository).save(existingUser);
        assertThat(updatedUser.getFirstName()).isEqualTo("Jane");
        assertThat(updatedUser.getLastName()).isEqualTo("Smith");
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() ->
                userService.updateUser(1L, "Jane", "Smith", UserRole.ADMIN)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldUpdatePassword() {
        // Given
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("old_encoded_password");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_password");

        // When
        userService.updatePassword(1L, "new_password");

        // Then
        verify(userRepository).save(existingUser);
        assertThat(existingUser.getPassword()).isEqualTo("new_encoded_password");
    }

    @Test
    void shouldDeleteUser() {
        // Given
        User existingUser = new User();
        existingUser.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).delete(existingUser);
    }

    @Test
    void shouldToggleUserEnabled() {
        // Given
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        // When
        userService.toggleUserEnabled(1L);

        // Then
        verify(userRepository).save(existingUser);
        assertThat(existingUser.isEnabled()).isFalse();
    }
}