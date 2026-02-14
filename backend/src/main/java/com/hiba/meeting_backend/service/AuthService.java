package com.hiba.meeting_backend.service;


import com.hiba.meeting_backend.DTO.LoginRequest;
import com.hiba.meeting_backend.Repository.UserRepository;
import com.hiba.meeting_backend.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }


    public User signup(LoginRequest input) {

            User user = new User();

         user.setEmail(input.getEmail());
         user.setPassword(passwordEncoder.encode(input.getPassword()));
         user.setName(input.getName());
         user.setRole("USER");
            return userRepository.save(user);
        }

        public User authenticate(LoginRequest input) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            input.getEmail(),
                            input.getPassword()
                    )
            );

            return userRepository.findByEmail(input.getEmail())
                    .orElseThrow();
        }
    }

