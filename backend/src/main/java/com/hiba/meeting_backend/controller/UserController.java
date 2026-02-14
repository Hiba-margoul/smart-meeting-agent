package com.hiba.meeting_backend.controller;

import com.hiba.meeting_backend.DTO.UserDTO;
import com.hiba.meeting_backend.model.User;
import com.hiba.meeting_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/users")
@RestController
@CrossOrigin(origins = "http://localhost:4200",
allowCredentials = "true")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User currentUser = (User) authentication.getPrincipal();

        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/")
    public ResponseEntity<List<UserDTO>> allUsers() {
        List <User> users = userService.allUsers();
        List<UserDTO> userDTOS = new ArrayList<>();
        users.forEach(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail(user.getEmail());
            userDTO.setId(user.getId());
            userDTO.setName(user.getName());
            userDTOS.add(userDTO);
        });
         System.out.println(users);
        return ResponseEntity.ok(userDTOS);
    }
    @GetMapping("/hiba")
    public ResponseEntity<String> hibaUsers() {
        return ResponseEntity.ok("hiba cv 3lik");
    }
}
