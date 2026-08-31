package com.Media.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.Media.model.UploadType;
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
            @RequestParam UploadType type, Principal principal) {
                System.err.println("-----------------------------------------");
        Map<String, String> response = mediaService.create(media, productId, type, principal.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getImage(@PathVariable String id) {
        Map<String, String> response = mediaService.getImage(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable String id, Principal principal) {
        Map<String, String> response = mediaService.deleteImage(id, principal.getName());
        return ResponseEntity.ok(response);
    }

}