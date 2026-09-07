package com.Media.service.events;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.Media.dto.event.ProductDeletedEvent;
import com.Media.model.Media;
import com.Media.repository.MediaRepository;
import com.Media.service.MediaService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventConsumer {
  private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    @KafkaListener(topics = "product-deleted-topic", groupId = "media-service-group")
    public void handleProductDeleted(ProductDeletedEvent event) {
        log.info("Kafka Event Received: Deleting all media for productId: {}", event.productId());

        List<Media> mediaList = mediaRepository.findByProductId(event.productId());

        if (mediaList.isEmpty()) {
            log.info("No media found for productId: {}", event.productId());
            return;
        }

        for (Media media : mediaList) {
            mediaService.deleteCloudinaryFileSafely(media);
        }

        mediaRepository.deleteAll(mediaList);
        log.info("Successfully deleted {} media items for productId: {}", mediaList.size(), event.productId());
    }
}