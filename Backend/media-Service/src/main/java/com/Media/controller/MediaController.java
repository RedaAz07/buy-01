package com.Media.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
            @RequestParam UploadType type, @RequestHeader("Authorization") String token, Principal principal) {
        Map<String, String> response = mediaService.create(media, productId, type, principal.getName(), token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getImage(@PathVariable String id) {
        Map<String, String> response = mediaService.getImage(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMedia(
            @RequestParam("url") String url,
            Principal principal) {

        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

        return ResponseEntity.ok(mediaService.deleteImageByUrl(decodedUrl, principal.getName()));
    }

}