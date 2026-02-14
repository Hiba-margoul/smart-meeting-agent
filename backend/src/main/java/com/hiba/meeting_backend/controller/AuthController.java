package com.hiba.meeting_backend.controller;

import com.hiba.meeting_backend.DTO.LoginRequest;
import com.hiba.meeting_backend.DTO.LoginResponse;
import com.hiba.meeting_backend.Repository.RefreshTokenRepository;
import com.hiba.meeting_backend.Repository.UserRepository;
import com.hiba.meeting_backend.model.RefreshToken;
import com.hiba.meeting_backend.model.User;
import com.hiba.meeting_backend.service.AuthService;
import com.hiba.meeting_backend.service.JwtService;
import com.hiba.meeting_backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200",
        allowCredentials = "true")

public class AuthController {
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(JwtService jwtService, AuthService authService, UserRepository userRepository,  RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;

    }

    @PostMapping("/signup")
    public ResponseEntity<User> signup ( @RequestBody LoginRequest loginRequest) {
        User user_signup = authService.signup(loginRequest);
        return ResponseEntity.ok(user_signup);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        User user_login = authService.authenticate(loginRequest);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user_login.getRole());
        String token = jwtService.generateToken(claims, user_login);

        String refreshToken = refreshTokenService.createRefreshToken(user_login.getId()).getToken();

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/auth/refresh")
                .maxAge(24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LoginResponse loginResponse_jwt = new LoginResponse();
        loginResponse_jwt.setToken(token);
        loginResponse_jwt.setExpiresIn(jwtService.getExpirationTime());

        return ResponseEntity.ok(loginResponse_jwt);
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token is missing. Please login again."));
        }
        Optional<RefreshToken> optionalToken = refreshTokenService.getRefreshToken(refreshTokenCookie);

        if (optionalToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token. Please login again."));
        }

        RefreshToken refreshToken = optionalToken.get();

        // Vérifier si le token a expiré
        if (refreshTokenService.isTokenExpired(refreshToken)) {
            refreshTokenService.deleteRefreshToken(refreshToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token expired. Please login again."));
        }

        // Générer un nouveau JWT
        String newAccessToken = jwtService.generateToken(refreshToken.getUser());

        LoginResponse response_jwt = new LoginResponse();
        response_jwt.setToken(newAccessToken);
        response_jwt.setExpiresIn(jwtService.getExpirationTime());

        // Retourner le token dans la réponse
        return ResponseEntity.ok(response_jwt);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie != null) {
            refreshTokenService.getRefreshToken(refreshTokenCookie)
                    .ifPresent(refreshTokenService::deleteRefreshToken);
        }

        // Supprimer le cookie côté client
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
