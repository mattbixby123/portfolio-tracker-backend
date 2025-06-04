package com.example.investment_portfolio_tracker.repository;

import com.example.investment_portfolio_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = 'USER'")
    List<User> findAllRegularUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    Long countRegularUsers();
}
