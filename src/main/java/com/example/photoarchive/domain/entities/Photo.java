package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document
public class Photo {
    @Id
    private String hash;
    private String name;
    private String folder;
    private Original original;
    private String status;
    private ExifData exifData;
    @ToString.Exclude()
    private String geocode;
    private ReadableGeocode readableGeocode;
    private PredictName predict;
    private String user;
    private LocalDateTime date;
}
