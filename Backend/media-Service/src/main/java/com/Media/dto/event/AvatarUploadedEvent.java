package com.Media.dto.event;

public record AvatarUploadedEvent(
    String userId,
    String avatarUrl
) {}