package com.example.course_management.entity;

// ... other imports ...

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <--- This import is failing

// ... other annotations ...
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
// Implement UserDetails interface
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // Used by UserDetails for authentication, should be email

    // **Change nullable to true or remove the annotation to allow null**
    @Column(nullable = true) // Or just remove the line: @Column
    private String password;

    // e.g., TEACHER, ADMIN, DIRECTION. Store without "ROLE_" prefix
    @Column(nullable = false)
    private String role;

    // Additional profile fields
    private String fullName;
    private String email; // This should ideally be the same as 'username' for login
    private String registrationToken; // Keep this for the invite flow

    // Map to isEnabled()
    private boolean isActivated = false; // Invited users are not activated initially

    // --- UserDetails methods implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Ensure role has "ROLE_" prefix for Spring Security
        String roleWithPrefix = this.role.startsWith("ROLE_") ? this.role : "ROLE_" + this.role;
        return Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix.toUpperCase()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Return the field used for authentication (email in your case)
        return email; // Or 'username' if that's what you use for login
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Simple implementation, adjust if you have account expiration logic
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Simple implementation, adjust if you have account locking logic
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Simple implementation, adjust if you have credential expiration logic
    }

    @Override
    public boolean isEnabled() {
        // Use the isActivated field
        return isActivated; // Invited users are not enabled until they complete registration
    }

    // Add getters for other fields if needed in other parts of the application
    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}