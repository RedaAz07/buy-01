package com.user_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.request.UpdateRequestDTO;
import com.user_service.dto.response.UpdateResponseDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMethodName(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDTO user = userService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateResponseDTO> updateUser(@RequestBody @Valid UpdateRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UpdateResponseDTO response = userService.updateUser(request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

}
