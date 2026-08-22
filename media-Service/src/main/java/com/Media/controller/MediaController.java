package com.Media.controller;

import java.util.List;
import java.util.Map;

import org.apache.tika.mime.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.Media.service.MediaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/media/images")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping()
    public ResponseEntity<Map<String, String>> create(
            @RequestPart("media") List<MultipartFile> media,
            @RequestParam(required = false) String productId,
            @RequestParam MediaType type) {
        Map<String, String> response = mediaService.create(media, productId, type);
        return ResponseEntity.ok(response);
    }

}