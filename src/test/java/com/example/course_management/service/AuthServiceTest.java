package com.example.course_management.service;

import com.example.course_management.entity.User;
import com.example.course_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Test
    void testRegisterNewUser() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setRole("STUDENT");
        user.setFullName("Test User");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User registeredUser = authService.register(user);

        // Then
        assertNotNull(registeredUser);
        verify(userRepository, times(1)).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();

        assertEquals("test@example.com", registeredUser.getUsername()); // This is because getUsername() returns the email
        assertEquals("test@example.com", capturedUser.getEmail());
        assertEquals("password123", capturedUser.getPassword());
        assertEquals("STUDENT", capturedUser.getRole());
        assertTrue(capturedUser.isActivated());
    }

    @Test
    void testCreateTeacherInvite_whenEmailAlreadyExists_shouldThrowRuntimeException() {
        // Given
        String existingEmail = "existing@example.com";
        String fullName = "Existing User";
        String token = "someToken";

        // Configure the mock UserRepository to return true for existsByEmail
        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            authService.createTeacherInvite(existingEmail, fullName, token);
        });

        // Verify the exception message
        assertTrue(thrown.getMessage().contains("Email already exists: " + existingEmail));

        // Verify that userRepository.save was never called
        verify(userRepository, never()).save(any(User.class));
    }
    //dezfezf
}
