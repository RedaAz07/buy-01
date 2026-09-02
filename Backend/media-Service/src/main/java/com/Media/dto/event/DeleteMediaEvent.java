package com.Media.dto.event;

import com.Media.model.UploadType;

public record DeleteMediaEvent(String userId, String productId, String imageUrl, UploadType type) {

}
