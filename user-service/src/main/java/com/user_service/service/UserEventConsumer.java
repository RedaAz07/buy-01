package com.user_service.service;

import com.user_service.dto.response.AvatarUploadedEvent;
import com.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = "avatar-uploaded-topic", groupId = "user-service-group")
    public void handleAvatarUploaded(AvatarUploadedEvent event) {
        log.info("Kafka Event Received: Updating avatar for username/userId: {}", event.userId());

        // Use findByUsername if event.userId() contains the username string
        userRepository.findByName(event.userId()).ifPresentOrElse(
            user -> {
                user.setAvatar(event.avatarUrl());
                userRepository.save(user);
                log.info("Successfully updated avatarUrl for user: {}", user.getName());
            },
            () -> log.warn("User not found for identifier: {}", event.userId())
        );
    }
}