package com.Media.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "media")
@Getter
@Setter
@NoArgsConstructor
public class Media {
    @Id
    private String id;
    private String imagePath;
    private String productId;
    private String ownerId;
    private UploadType type;

}