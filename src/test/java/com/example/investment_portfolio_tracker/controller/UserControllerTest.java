package com.example.investment_portfolio_tracker.controller;

import com.example.investment_portfolio_tracker.dto.UserDto;
import com.example.investment_portfolio_tracker.model.User;
import com.example.investment_portfolio_tracker.model.UserRole;
import com.example.investment_portfolio_tracker.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(UserControllerTestConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private Authentication authentication;

    private User regularUser;
    private User adminUser;
    private UserDto userDto;
    private UserDto adminUserDto;

    @BeforeEach
    void setUp() {
        // Set up regular user
        regularUser = new User();
        regularUser.setId(1L);
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("password");
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setRole(UserRole.USER);
        regularUser.setEnabled(true);

        // Set up admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);

        // Set up user DTO for requests
        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("user@example.com");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setRole(UserRole.USER);
        userDto.setEnabled(true);

        // Set up admin DTO
        adminUserDto = new UserDto();
        adminUserDto.setId(2L);
        adminUserDto.setEmail("admin@example.com");
        adminUserDto.setFirstName("Admin");
        adminUserDto.setLastName("User");
        adminUserDto.setRole(UserRole.ADMIN);
        adminUserDto.setEnabled(true);
    }

    @Test
    void shouldGetUserProfile() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        // When/Then
        mockMvc.perform(get("/api/v1/users/profile")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.email", is("user@example.com")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.role", is("USER")))
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    void shouldReturnErrorWhenUserNotFound() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("unknown@example.com");
        given(userService.getUserByEmail("unknown@example.com")).willReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/v1/users/profile")
                        .principal(authentication))
                .andExpect(status().is5xxServerError()); // IllegalArgumentException results in 500
    }

    @Test
    void shouldUpdateUserProfile() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        UserDto updatedDto = new UserDto();
        updatedDto.setFirstName("Updated");
        updatedDto.setLastName("Name");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("user@example.com");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setRole(UserRole.USER);
        updatedUser.setEnabled(true);

        given(userService.updateUser(eq(1L), eq("Updated"), eq("Name"), eq(UserRole.USER)))
                .willReturn(updatedUser);

        // When/Then
        mockMvc.perform(put("/api/v1/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Name")));
    }

    @Test
    void shouldChangePassword() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("newPassword", "newPassword123!");

        doNothing().when(userService).updatePassword(1L, "newPassword123!");

        // When/Then
        mockMvc.perform(post("/api/v1/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordData))
                        .principal(authentication))
                .andExpect(status().isOk());

        verify(userService).updatePassword(1L, "newPassword123!");
    }

    @Test
    void shouldGetAllUsersAsAdmin() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("admin@example.com");
        given(userService.getUserByEmail("admin@example.com")).willReturn(Optional.of(adminUser));

        List<User> userList = Arrays.asList(regularUser, adminUser);
        given(userService.getAllUsers()).willReturn(userList);

        // When/Then
        mockMvc.perform(get("/api/v1/users/admin/all")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email", is("user@example.com")))
                .andExpect(jsonPath("$[1].email", is("admin@example.com")));
    }

    @Test
    void shouldForbidGetAllUsersAsRegularUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        // When/Then
        mockMvc.perform(get("/api/v1/users/admin/all")
                        .principal(authentication))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAdminUpdateUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("admin@example.com");
        given(userService.getUserByEmail("admin@example.com")).willReturn(Optional.of(adminUser));

        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        updateDto.setRole(UserRole.ADMIN); // Admin can change role

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("user@example.com");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setRole(UserRole.ADMIN);
        updatedUser.setEnabled(true);

        given(userService.updateUser(eq(1L), eq("Updated"), eq("Name"), eq(UserRole.ADMIN)))
                .willReturn(updatedUser);

        // When/Then
        mockMvc.perform(put("/api/v1/users/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated")))
                .andExpect(jsonPath("$.lastName", is("Name")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    void shouldForbidAdminUpdateUserAsRegularUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        // When/Then
        mockMvc.perform(put("/api/v1/users/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto))
                        .principal(authentication))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldToggleUserEnabled() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("admin@example.com");
        given(userService.getUserByEmail("admin@example.com")).willReturn(Optional.of(adminUser));
        doNothing().when(userService).toggleUserEnabled(1L);

        // When/Then
        mockMvc.perform(post("/api/v1/users/admin/1/toggle-enabled")
                        .principal(authentication))
                .andExpect(status().isOk());

        verify(userService).toggleUserEnabled(1L);
    }

    @Test
    void shouldForbidToggleUserEnabledAsRegularUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        // When/Then
        mockMvc.perform(post("/api/v1/users/admin/1/toggle-enabled")
                        .principal(authentication))
                .andExpect(status().isForbidden());

        verify(userService, never()).toggleUserEnabled(anyLong());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("admin@example.com");
        given(userService.getUserByEmail("admin@example.com")).willReturn(Optional.of(adminUser));
        doNothing().when(userService).deleteUser(1L);

        // When/Then
        mockMvc.perform(delete("/api/v1/users/admin/1")
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void shouldNotAllowAdminToDeleteSelf() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("admin@example.com");
        given(userService.getUserByEmail("admin@example.com")).willReturn(Optional.of(adminUser));

        // When/Then
        mockMvc.perform(delete("/api/v1/users/admin/2") // Admin's own ID
                        .principal(authentication))
                .andExpect(status().isBadRequest());

        verify(userService, never()).deleteUser(anyLong());
    }

    @Test
    void shouldForbidDeleteUserAsRegularUser() throws Exception {
        // Given
        when(authentication.getName()).thenReturn("user@example.com");
        given(userService.getUserByEmail("user@example.com")).willReturn(Optional.of(regularUser));

        // When/Then
        mockMvc.perform(delete("/api/v1/users/admin/1")
                        .principal(authentication))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyLong());
    }
}