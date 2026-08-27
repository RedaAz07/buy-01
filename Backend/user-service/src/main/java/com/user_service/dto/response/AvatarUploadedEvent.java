package com.user_service.dto.response;
public record AvatarUploadedEvent(
    String userId,
    String avatarUrl
) {}