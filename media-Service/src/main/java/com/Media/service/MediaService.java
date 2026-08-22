package com.Media.service;

import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();

    public Map<String, String> create(List<MultipartFile> media, String productId, MediaType type) {

        private static final List<String> ALLOWED_MIME_TYPES = List.of(
                "image/jpeg",
                "image/png",
                "image/webp");

        return Map.of("message", "success");

    }

}