package com.user_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user_service.dto.request.UpdateRequestDTO;
import com.user_service.dto.response.UpdateResponseDTO;
import com.user_service.dto.response.UserResponseDTO;
import com.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.security.Principal;

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
    public ResponseEntity<UserResponseDTO> getMethodName(Principal principal) {
        UserResponseDTO user = userService.getMe(principal.getName());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UpdateResponseDTO> updateUser(@RequestBody @Valid UpdateRequestDTO request,
           Principal principal) {
        UpdateResponseDTO response = userService.updateUser(request, principal.getName());
        return ResponseEntity.ok(response);
    }

}
