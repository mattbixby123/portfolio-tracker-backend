package com.example.investment_portfolio_tracker.dto;

import com.example.investment_portfolio_tracker.model.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private boolean enabled;

    // Explicitly exclude sensitive information from serialization
    @JsonIgnore
    private String password;
}