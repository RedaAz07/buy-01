package com.user_service.service;

import com.user_service.dto.response.AvatarUploadedEvent;
import com.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "avatar-uploaded-topic", groupId = "user-service-group")
    public void handleAvatarUploaded(AvatarUploadedEvent event) {

        // Use findByUsername if event.userId() contains the username string
        userRepository.findById(event.userId()).ifPresentOrElse(
            user -> {
                user.setAvatar(event.avatarUrl());
                userRepository.save(user);
            },
            () -> System.err.println("salam"));
        
    }
}