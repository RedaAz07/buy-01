package com.user_service.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.request.AvatarRequestDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO user = userService.me(userDetails.getUsername());
        return ResponseEntity.ok(user);

    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable String id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);

    }

    @PutMapping()
    public ResponseEntity<UserResponseDTO> updateAvatar(@RequestBody AvatarRequestDTO avatar, @AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO user = userService.updateAvatar(avatar, userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

}